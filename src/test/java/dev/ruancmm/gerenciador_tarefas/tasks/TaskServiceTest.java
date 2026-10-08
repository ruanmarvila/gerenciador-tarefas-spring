package dev.ruancmm.gerenciador_tarefas.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import dev.ruancmm.gerenciador_tarefas.core.exception.AuthorizationException;
import dev.ruancmm.gerenciador_tarefas.tasks.dto.request.TaskCreateRequest;
import dev.ruancmm.gerenciador_tarefas.tasks.dto.request.TaskFilterRequest;
import dev.ruancmm.gerenciador_tarefas.tasks.dto.request.TaskUpdateRequest;
import dev.ruancmm.gerenciador_tarefas.tasks.dto.response.TaskResponse;
import dev.ruancmm.gerenciador_tarefas.tasks.exception.TaskNotFoundException;
import dev.ruancmm.gerenciador_tarefas.users.User;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User("Test", "test@test.com", "testtest");
        currentUser.setId(1L);
    }

    @Test
    void shouldCreateTask() {
        TaskCreateRequest request = new TaskCreateRequest("Test", "Testing");
        TaskResponse response = new TaskResponse(1L, "Test", "Testing", TaskStatus.TODO);

        Task task = new Task(currentUser, "Test", "Testing");
        task.setId(1L);

        when(taskRepository.save(any(Task.class)))
            .thenReturn(task);

        TaskResponse result = taskService.create(request, currentUser);

        assertEquals(response, result);

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void shouldListTasksWithFilterAndMapToResponse() {
        TaskFilterRequest filter = new TaskFilterRequest("Spring", null, null);
        Pageable pageable = PageRequest.of(0, 10);

        Task task = new Task(currentUser, "Study Spring", "Finish tests");
        Page<Task> taskPage = new PageImpl<>(List.of(task));

        when(taskRepository.findAll(ArgumentMatchers.<Specification<Task>>any(), eq(pageable)))
            .thenReturn(taskPage);

        Page<TaskResponse> result = taskService.list(filter, pageable, currentUser.getId());

        assertEquals(1, result.getTotalElements());
        assertEquals("Study Spring", result.getContent().get(0).title());

        verify(taskRepository).findAll(ArgumentMatchers.<Specification<Task>>any(), eq(pageable));
    }

    @Test
    void shouldListDeletedTasks() {
        Pageable pageable = PageRequest.of(0, 10);

        Task task = new Task(currentUser, "Deleted Task", null);
        task.setDeletedAt(LocalDateTime.now());

        Page<Task> taskPage = new PageImpl<>(List.of(task));

        when(taskRepository.findAllDeletedByUserId(any(), eq(pageable)))
            .thenReturn(taskPage);

        Page<TaskResponse> result = taskService.listDeleted(pageable, currentUser.getId());

        assertEquals(1, result.getTotalElements());
        assertEquals("Deleted Task", result.getContent().get(0).title());

        verify(taskRepository).findAllDeletedByUserId(any(), eq(pageable));
    }

    @Test
    void shouldGetTask() {
        Task task = new Task(currentUser, "Test Task", "...");
        task.setId(1L);
        TaskResponse response = new TaskResponse(1L, "Test Task", "...", TaskStatus.TODO);

        when(taskRepository.findById(any()))
            .thenReturn(Optional.of(task));
        
        TaskResponse result = taskService.getById(1L, currentUser);

        assertEquals(response, result);

        verify(taskRepository).findById(any());
    }

    @Test
    void shouldRejectWhenTaskNotFound() {
        when(taskRepository.findById(any()))
            .thenThrow(new TaskNotFoundException());
        
        assertThrows(
            TaskNotFoundException.class,
            () -> taskService.getById(1L, currentUser)
        );
    }

    @Test
    void shouldRejectWhenTaskDoesNotBelongCurrentUser() {
        User otherUser = new User();
        otherUser.setId(2L);

        Task task = new Task(otherUser, "Test Task", "...");
        task.setId(1L);

        when(taskRepository.findById(any()))
            .thenReturn(Optional.of(task));
        
        assertThrows(
            AuthorizationException.class,
            () -> taskService.getById(1L, currentUser)
        );
    }

    @Test
    void shouldUpdateTask() {
        TaskUpdateRequest request = new TaskUpdateRequest(
            "New title", "New description", TaskStatus.DONE
        );
        TaskResponse response = new TaskResponse(1L, "New title", "New description", TaskStatus.DONE);

        Task task = new Task(currentUser, "Old title", "Old description");
        task.setId(1L);

        when(taskRepository.findById(any()))
            .thenReturn(Optional.of(task));

        when(taskRepository.save(any(Task.class)))
            .thenReturn(task);
        
        TaskResponse result = taskService.update(request, 1L, currentUser);

        assertEquals(response, result);

        verify(taskRepository).save(argThat(savedTask -> 
            savedTask.getTitle().equals(request.title()) &&
            savedTask.getDescription().equals(request.description()) &&
            savedTask.getStatus() == request.status()
        ));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldKeepTitleWhenTitleIsNull(String title) {
        TaskUpdateRequest request = new TaskUpdateRequest(title, "New description", TaskStatus.DONE);
        TaskResponse response = new TaskResponse(1L, "Old title", "New description", TaskStatus.DONE);

        Task task = new Task(currentUser, "Old title", "Old description");
        task.setId(1L);

        when(taskRepository.findById(any()))
            .thenReturn(Optional.of(task));

        when(taskRepository.save(any(Task.class)))
            .thenReturn(task);
        
        TaskResponse result = taskService.update(request, 1L, currentUser);

        assertEquals(response, result);
        assertEquals("Old title", task.getTitle());
        assertEquals("New description", task.getDescription());
        assertEquals(TaskStatus.DONE, task.getStatus());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldKeepDescriptionWhenDescriptionIsNull(String description) {
        TaskUpdateRequest request = new TaskUpdateRequest("New title", description, TaskStatus.DONE);
        TaskResponse response = new TaskResponse(1L, "New title", "Old description", TaskStatus.DONE);

        Task task = new Task(currentUser, "Old title", "Old description");
        task.setId(1L);

        when(taskRepository.findById(any()))
            .thenReturn(Optional.of(task));

        when(taskRepository.save(any(Task.class)))
            .thenReturn(task);
        
        TaskResponse result = taskService.update(request, 1L, currentUser);

        assertEquals(response, result);
        assertEquals("New title", task.getTitle());
        assertEquals("Old description", task.getDescription());
        assertEquals(TaskStatus.DONE, task.getStatus());
    }

    @Test
    void shouldKeepStatusWhenStatusIsNull() {
        TaskUpdateRequest request = new TaskUpdateRequest("New title", "New description", null);
        TaskResponse response = new TaskResponse(1L, "New title", "New description", TaskStatus.TODO);

        Task task = new Task(currentUser, "Old title", "Old description");
        task.setId(1L);

        when(taskRepository.findById(any()))
            .thenReturn(Optional.of(task));

        when(taskRepository.save(any(Task.class)))
            .thenReturn(task);
        
        TaskResponse result = taskService.update(request, 1L, currentUser);

        assertEquals(response, result);
        assertEquals("New title", task.getTitle());
        assertEquals("New description", task.getDescription());
        assertEquals(TaskStatus.TODO, task.getStatus());
    }

    @Test
    void shouldRejectUpdateWhenTaskNotFound() {
        TaskUpdateRequest request = new TaskUpdateRequest("Title", "Description", TaskStatus.DONE);

        when(taskRepository.findById(any()))
            .thenReturn(Optional.empty());
        
        assertThrows(
            TaskNotFoundException.class,
            () -> taskService.update(request, 1L, currentUser)
        );
    }

    @Test
    void shouldRejectUpdateWhenTaskDoesNotBelongCurrentUser() {
        User otherUser = new User();
        otherUser.setId(2L);

        TaskUpdateRequest request = new TaskUpdateRequest(
            "New title", "New description", TaskStatus.DONE
        );
        Task task = new Task(otherUser, "Old title", "Old description");

        when(taskRepository.findById(any()))
            .thenReturn(Optional.of(task));
        
        assertThrows(
            AuthorizationException.class,
            () -> taskService.update(request, 1L, currentUser)
        );
    }

    @Test
    void shouldRestoreTask() {
        Task task = new Task(currentUser, "Restored", null);
        task.setId(1L);
        task.setDeletedAt(LocalDateTime.now());
        TaskResponse response = new TaskResponse(1L, "Restored", "description", TaskStatus.TODO);

        when(taskRepository.findDeletedById(any()))
            .thenReturn(Optional.of(task));
        
        when(taskRepository.save(any(Task.class)))
            .thenReturn(task);

        TaskResponse result = taskService.restore(1L, currentUser);

        assertNull(task.getDeletedAt());
        assertEquals(response, result);

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void shouldRejectRestoreWhenTaskNotFound() {

        when(taskRepository.findDeletedById(any()))
            .thenReturn(Optional.empty());
        
        assertThrows(
            TaskNotFoundException.class,
            () -> taskService.restore(1L, currentUser)
        );
    }

    @Test
    void shouldRejectRestoreWhenTaskDoesNotBelongCurrentUser() {
        User otherUser = new User();
        otherUser.setId(2L);

        Task task = new Task(otherUser, null, null);
        task.setId(1L);
        task.setDeletedAt(LocalDateTime.now());

        when(taskRepository.findDeletedById(any()))
            .thenReturn(Optional.of(task));
        
        assertThrows(
            AuthorizationException.class,
            () -> taskService.restore(1L, currentUser)
        );
    }

    @Test 
    void shouldDeleteTask() {
        Task task = new Task(currentUser, null, null);

        when(taskRepository.findById(any()))
            .thenReturn(Optional.of(task));
        
        taskService.delete(1L, currentUser);

        verify(taskRepository).delete(task);
    }

    @Test
    void shouldRejectDeleteWhenTaskNotFound() {
        when(taskRepository.findById(any()))
            .thenReturn(Optional.empty());
        
        assertThrows(
            TaskNotFoundException.class,
            () -> taskService.delete(1L, currentUser)
        );
    }

    @Test
    void shouldRejectDeleteWhenTaskDoesNotBelongCurrentUser() {
        User otherUser = new User();
        otherUser.setId(2L);

        Task task = new Task(otherUser, null, null);

        when(taskRepository.findById(any()))
            .thenReturn(Optional.of(task));
        
        assertThrows(
            AuthorizationException.class,
            () -> taskService.delete(1L, currentUser)
        );
    }

    @Test
    void shouldDeleteAllDeletedTasks() {
        taskService.emptyTrash(currentUser.getId());

        verify(taskRepository).deleteAllDeletedByUserId(currentUser.getId());
    }
}
