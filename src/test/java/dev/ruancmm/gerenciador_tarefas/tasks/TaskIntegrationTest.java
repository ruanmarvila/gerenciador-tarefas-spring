package dev.ruancmm.gerenciador_tarefas.tasks;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import dev.ruancmm.gerenciador_tarefas.config.PostgresContainerConfig;
import dev.ruancmm.gerenciador_tarefas.support.AuthTestHelper;
import dev.ruancmm.gerenciador_tarefas.support.AuthTestHelper.Session;
import static dev.ruancmm.gerenciador_tarefas.support.AuthTestHelper.uniqueEmail;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
public class TaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthTestHelper auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestHelper(mockMvc, objectMapper);
    }

    @Test
    void shouldAllowOwnerToDeleteOwnTask() throws Exception {
        Session session = auth.registerAndLogin(
            "Owner", uniqueEmail(), "ownerPassword"
        );
        long taskId = auth.createTask(session.accessToken(), "Owner's task");

        mockMvc.perform(delete("/tasks/{id}", taskId)
                .header("Authorization", "Bearer " + session.accessToken()))
            .andExpect(status().isNoContent());
    }
    
    @Test
    void shouldReturn404WhenUserTriesToDeleteAnotherUsersTask() throws Exception {
        Session ownerSession = auth.registerAndLogin(
            "Owner", uniqueEmail(), "ownerPassword"
        );
        Session intruderSession = auth.registerAndLogin(
            "Intruder", uniqueEmail(), "intruderPassword"
        );
        long taskId = auth.createTask(ownerSession.accessToken(), "Owner's task");

        mockMvc.perform(delete("/tasks/{id}", taskId)
                .header("Authorization", "Bearer " + intruderSession.accessToken()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Task not found"))
            .andExpect(jsonPath("$.timestamp").exists());

        mockMvc.perform(get("/tasks/{id}", taskId)
                .header("Authorization", "Bearer " + ownerSession.accessToken()))
            .andExpect(status().isOk());
    }

    @Test
    void shouldAllowOwnerToRestoreOwnTask() throws Exception {
        Session ownerSession = auth.registerAndLogin(
            "Owner", uniqueEmail(), "ownerPassword"
        );
        long taskId = auth.createTask(ownerSession.accessToken(), "Owner's Task");

        mockMvc.perform(delete("/tasks/{id}", taskId)
            .header("Authorization", "Bearer " + ownerSession.accessToken()))
        .andExpect(status().isNoContent());

        mockMvc.perform(post("/tasks/{id}/restore", taskId)
            .header("Authorization", "Bearer " + ownerSession.accessToken()))
        .andExpect(status().isOk());

        mockMvc.perform(get("/tasks/{id}", taskId)
            .header("Authorization", "Bearer " + ownerSession.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Owner's Task"))
        .andExpect(jsonPath("$.description").value("Created by integration test"))
        .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void shouldPaginateOnlyTheAuthenticatedUsersTasks() throws Exception {
        Session ownerSession = auth.registerAndLogin("Owner", uniqueEmail(), "ownerPassword");
        Session otherSession = auth.registerAndLogin("Other", uniqueEmail(), "otherPassword");

        for (int i = 1; i <= 25; i++) {
            auth.createTask(ownerSession.accessToken(), "Task " + i);
        }

        for (int i = 1; i <= 5; i++) {
            auth.createTask(otherSession.accessToken(), "Other " + i);
        }

        mockMvc.perform(get("/tasks?page=0&size=20")
            .header("Authorization", "Bearer " + ownerSession.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(20))
        .andExpect(jsonPath("$.page.totalElements").value(25))
        .andExpect(jsonPath("$.page.totalPages").value(2));
        
        mockMvc.perform(get("/tasks?page=1&size=5")
            .header("Authorization", "Bearer " + ownerSession.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(5));
    }

    @Test
    void shouldCapPageSizeAtTheMaximum() throws Exception {
        Session session = auth.registerAndLogin("Owner", uniqueEmail(), "ownerPassword");
        auth.createTask(session.accessToken(), "Only task");

        mockMvc.perform(get("/tasks?size=1000")
                .header("Authorization", "Bearer " + session.accessToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size").value(50));
    }

    @Test
    void shouldReturn400WhenSortPropertyIsInvalid() throws Exception {
        Session session = auth.registerAndLogin("Owner", uniqueEmail(), "ownerPassword");

        mockMvc.perform(get("/tasks?sort=isDeleted,desc")
                .header("Authorization", "Bearer " + session.accessToken()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSortByTitleAscending() throws Exception {
        Session session = auth.registerAndLogin("Owner", uniqueEmail(), "ownerPassword");
        auth.createTask(session.accessToken(), "Banana");
        auth.createTask(session.accessToken(), "Apple");

        mockMvc.perform(get("/tasks?sort=title,asc")
                .header("Authorization", "Bearer " + session.accessToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].title").value("Apple"))
            .andExpect(jsonPath("$.content[1].title").value("Banana"));
    }

    @Test
    void shouldNotListTasksFromTheTrash() throws Exception {
        Session session = auth.registerAndLogin("Owner", uniqueEmail(), "ownerPassword");
        auth.createTask(session.accessToken(), "Keep");
        long deletedId = auth.createTask(session.accessToken(), "Delete me");

        mockMvc.perform(delete("/tasks/{id}", deletedId)
                .header("Authorization", "Bearer " + session.accessToken()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/tasks")
                .header("Authorization", "Bearer " + session.accessToken()))
            .andExpect(jsonPath("$.page.totalElements").value(1));
    }
}
