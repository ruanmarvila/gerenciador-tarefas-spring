package dev.ruancmm.gerenciador_tarefas.tasks;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import dev.ruancmm.gerenciador_tarefas.auth.JwtUtil;
import dev.ruancmm.gerenciador_tarefas.core.exception.AuthorizationException;
import dev.ruancmm.gerenciador_tarefas.core.security.SecurityConfig;
import dev.ruancmm.gerenciador_tarefas.tasks.dto.request.TaskCreateRequest;
import dev.ruancmm.gerenciador_tarefas.tasks.dto.request.TaskUpdateRequest;
import dev.ruancmm.gerenciador_tarefas.tasks.dto.request.TaskFilterRequest;
import dev.ruancmm.gerenciador_tarefas.tasks.dto.response.TaskResponse;
import dev.ruancmm.gerenciador_tarefas.tasks.exception.TaskNotFoundException;
import dev.ruancmm.gerenciador_tarefas.users.User;
import dev.ruancmm.gerenciador_tarefas.users.UserRepository;

@Import(SecurityConfig.class)
@WebMvcTest(TaskController.class)
public class TaskControllerTest {

    @Autowired 
    private MockMvc mockMvc;

    @MockitoBean 
    private TaskService taskService;

    @MockitoBean 
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    private final User mockUser = new User("Test", "test@test.com", "testtest");

    @Test
    void shouldCreateTask() throws Exception {
        TaskResponse response = new TaskResponse(
            1L, "Study Spring", "Finish tests", TaskStatus.IN_PROGRESS
        );

        when(taskService.create(any(TaskCreateRequest.class), eq(mockUser)))
            .thenReturn(response);
        
        mockMvc.perform(post("/tasks")
            .with(user(mockUser))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "Study Spring",
                    "description": "Finish tests"
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Study Spring"))
            .andExpect(jsonPath("$.description").value("Finish tests"))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        
        verify(taskService).create(any(TaskCreateRequest.class), eq(mockUser));
    }

    @Test
    void shouldReturn401WhenWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "Study Spring",
                    "description": "Finish tests"
                }
                """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldListTasks() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        
        TaskResponse response = new TaskResponse(
            1L, "Study Spring", "Finish tests", TaskStatus.IN_PROGRESS
        );
        Page<TaskResponse> taskResponsePage = new PageImpl<>(List.of(response), pageable, 1);

        when(taskService.list(any(TaskFilterRequest.class), any(Pageable.class), any()))
            .thenReturn(taskResponsePage);
        
        mockMvc.perform(get("/tasks")
            .param("page", "0")
            .param("size", "10")
            .with(user(mockUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.content[0].title").value("Study Spring"))
        .andExpect(jsonPath("$.content[0].description").value("Finish tests"))
        .andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.number").value(0));
        
        verify(taskService).list(any(TaskFilterRequest.class), any(Pageable.class), any());
    }

    @Test
    void shouldListDeletedTasks() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        
        TaskResponse response = new TaskResponse(
            1L, "Delete Task", "Test delete", TaskStatus.DONE
        );
        Page<TaskResponse> taskResponsePage = new PageImpl<>(List.of(response), pageable, 1);

        when(taskService.listDeleted(any(Pageable.class), any()))
            .thenReturn(taskResponsePage);
        
        mockMvc.perform(get("/tasks/trash")
            .param("page", "0")
            .param("size", "10")
            .with(user(mockUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.content[0].title").value("Delete Task"))
        .andExpect(jsonPath("$.content[0].description").value("Test delete"))
        .andExpect(jsonPath("$.content[0].status").value("DONE"))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.number").value(0));
        
        verify(taskService).listDeleted(any(Pageable.class), any());
    }

    @Test
    void shouldGetTask() throws Exception {
        TaskResponse response = new TaskResponse(1L, "Test", "testing", TaskStatus.IN_PROGRESS);

        when(taskService.getById(eq(1L), eq(mockUser)))
            .thenReturn(response);
        
        mockMvc.perform(get("/tasks/{id}", 1L)
            .with(user(mockUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Test"))
        .andExpect(jsonPath("$.description").value("testing"))
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void shouldReturn403WhenUserIsNotAllowed() throws Exception {
        doThrow(new AuthorizationException())
            .when(taskService).getById(eq(1L), eq(mockUser));
        
        mockMvc.perform(get("/tasks/{id}", 1L)
            .with(user(mockUser)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.message").value("You don't have authorization for this operation"))
        .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn404WhenTaskNotFound() throws Exception {
        doThrow(new TaskNotFoundException())
            .when(taskService).getById(eq(1L), eq(mockUser));
        
        mockMvc.perform(get("/tasks/{id}", 1L)
            .with(user(mockUser)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("Task not found"))
        .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldUpdateTask() throws Exception {
        TaskResponse response = new TaskResponse(1L, "New Title", "New Description", TaskStatus.DONE);

        when(taskService.update(any(TaskUpdateRequest.class), eq(1L), eq(mockUser)))
            .thenReturn(response);
        
        mockMvc.perform(patch("/tasks/{id}", 1L)
            .with(user(mockUser))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "New Title",
                    "description": "New Description",
                    "status": "DONE"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("New Title"))
            .andExpect(jsonPath("$.description").value("New Description"))
            .andExpect(jsonPath("$.status").value("DONE"));
        
            verify(taskService).update(any(TaskUpdateRequest.class), eq(1L), eq(mockUser));
    }

    @Test
    void shouldReturn403WhenUserIsNotAllowedToUpdateTask() throws Exception {
        doThrow(new AuthorizationException())
            .when(taskService).update(any(TaskUpdateRequest.class), eq(1L), eq(mockUser));

        mockMvc.perform(patch("/tasks/{id}", 1L)
            .with(user(mockUser))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "New Title",
                    "description": "New Description",
                    "status": "DONE"
                }
                """))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenTaskNotFoundOnUpdate() throws Exception {
        doThrow(new TaskNotFoundException())
            .when(taskService).update(any(TaskUpdateRequest.class), eq(1L), eq(mockUser));

        mockMvc.perform(patch("/tasks/{id}", 1L)
            .with(user(mockUser))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "New Title",
                    "description": "New Description",
                    "status": "DONE"
                }
                """))
        .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn422WhenUpdateRequestIsNull() throws Exception {
        mockMvc.perform(patch("/tasks/{id}", 1L)
            .with(user(mockUser))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": null,
                    "description": null,
                    "status": null
                }
                """))
            .andExpect(status().isUnprocessableContent());
    }

    @Test
    void shouldDeleteTask() throws Exception {
        mockMvc.perform(delete("/tasks/{id}", 1L)
            .with(user(mockUser)))
        .andExpect(status().isNoContent());

        verify(taskService).delete(eq(1L), eq(mockUser));
    }

    @Test
    void shouldReturn403WhenUserIsNotAllowedToDeleteTask() throws Exception {
        doThrow(new AuthorizationException())
            .when(taskService).delete(eq(1L), eq(mockUser));

        mockMvc.perform(delete("/tasks/{id}", 1L)
            .with(user(mockUser)))
        .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenTaskNotFoundOnDelete() throws Exception {
        doThrow(new TaskNotFoundException())
            .when(taskService).delete(eq(1L), eq(mockUser));
        
        mockMvc.perform(delete("/tasks/{id}", 1L)
            .with(user(mockUser)))
        .andExpect(status().isNotFound());
    }

    @Test
    void shouldRestoretask() throws Exception {
        TaskResponse response = new TaskResponse(1L, "Restored", "restore a task", TaskStatus.TODO);

        when(taskService.restore(eq(1L), eq(mockUser)))
            .thenReturn(response);
        
        mockMvc.perform(post("/tasks/{id}/restore", 1L)
            .with(user(mockUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Restored"))
        .andExpect(jsonPath("$.description").value("restore a task"))
        .andExpect(jsonPath("$.status").value("TODO"));

        verify(taskService).restore(eq(1L), eq(mockUser));
    }

    @Test
    void shouldReturn403WhenUserIsNotAllowedToRestoreTask() throws Exception {
        doThrow(new AuthorizationException())
            .when(taskService).restore(eq(1L), eq(mockUser));

        mockMvc.perform(post("/tasks/{id}/restore", 1L)
            .with(user(mockUser)))
        .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenTaskNotFoundOnRestore() throws Exception {
        doThrow(new TaskNotFoundException())
            .when(taskService).restore(eq(1L), eq(mockUser));

        mockMvc.perform(post("/tasks/{id}/restore", 1L)
            .with(user(mockUser)))
        .andExpect(status().isNotFound());
    }

    @Test
    void shouldEmptyTrash() throws Exception {
        mockMvc.perform(delete("/tasks/trash")
            .with(user(mockUser)))
        .andExpect(status().isNoContent());

        verify(taskService).emptyTrash(any());
    }
}
