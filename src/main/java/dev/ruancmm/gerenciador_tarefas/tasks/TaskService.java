package dev.ruancmm.gerenciador_tarefas.tasks;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import dev.ruancmm.gerenciador_tarefas.core.exception.AuthorizationException;
import dev.ruancmm.gerenciador_tarefas.tasks.dto.request.TaskCreateRequest;
import dev.ruancmm.gerenciador_tarefas.tasks.dto.request.TaskFilterRequest;
import dev.ruancmm.gerenciador_tarefas.tasks.dto.request.TaskUpdateRequest;
import dev.ruancmm.gerenciador_tarefas.tasks.dto.response.TaskResponse;
import dev.ruancmm.gerenciador_tarefas.tasks.exception.TaskNotFoundException;
import dev.ruancmm.gerenciador_tarefas.users.User;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public TaskResponse create(TaskCreateRequest request, User currentUser) {
        Task task = new Task(currentUser, request.title(), request.description());
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    public Page<TaskResponse> list(TaskFilterRequest filter, Pageable pageable, Long userId) {
        return taskRepository.findAll(TaskSpecification.withFilter(filter, userId), pageable)
            .map(TaskResponse::fromEntity);
    }

    public TaskResponse update(TaskUpdateRequest request, Long id, User currentUser) {
        Task task = taskRepository.findById(id)
            .orElseThrow(TaskNotFoundException::new);
        
        if (task.getUser().getId() != currentUser.getId()) {
            throw new AuthorizationException();
        }

        if (request.title() != null && !request.title().isBlank()) {
            task.setTitle(request.title());
        }

        if (request.description() != null && !request.description().isBlank()) {
            task.setDescription(request.description());
        }

        if (request.status() != null) {
            task.setStatus(request.status());
        }

        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    public void delete(Long id, User currentUser) {
        Task task = taskRepository.findById(id)
            .orElseThrow(TaskNotFoundException::new);
        
        if (task.getUser().getId() != currentUser.getId()) {
            throw new AuthorizationException();
        }

        taskRepository.delete(task);
    }

}
