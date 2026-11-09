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
}
