package dev.ruancmm.gerenciador_tarefas.tasks;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Page<TaskResponse> listDeleted(Pageable pageable, Long userId) {
        return taskRepository.findAllDeletedByUserId(userId, pageable)
            .map(TaskResponse::fromEntity);
    }

    public TaskResponse getById(Long id, User currentUser) {
        return taskRepository.findByIdAndUserId(id, currentUser.getId())
            .map(TaskResponse::fromEntity)
            .orElseThrow(TaskNotFoundException::new);
    }

    public TaskResponse update(TaskUpdateRequest request, Long id, User currentUser) {
        Task task = taskRepository.findByIdAndUserId(id, currentUser.getId())
            .orElseThrow(TaskNotFoundException::new);

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

    @Transactional
    public TaskResponse restore(Long id, User currentUser) {
        Task task = taskRepository.findDeletedByIdAndUserId(id, currentUser.getId())
            .orElseThrow(TaskNotFoundException::new);

        task.setDeletedAt(null);
        return TaskResponse.fromEntity(task);
    }

    public void delete(Long id, User currentUser) {
        Task task = taskRepository.findByIdAndUserId(id, currentUser.getId())
            .orElseThrow(TaskNotFoundException::new);

        taskRepository.delete(task);
    }

    @Transactional
    public void emptyTrash(Long userId) {
        taskRepository.deleteAllDeletedByUserId(userId);
    }

}
