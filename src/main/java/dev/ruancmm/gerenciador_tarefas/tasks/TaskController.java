package dev.ruancmm.gerenciador_tarefas.tasks;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.ruancmm.gerenciador_tarefas.tasks.dto.request.TaskCreateRequest;
import dev.ruancmm.gerenciador_tarefas.tasks.dto.request.TaskFilterRequest;
import dev.ruancmm.gerenciador_tarefas.tasks.dto.request.TaskUpdateRequest;
import dev.ruancmm.gerenciador_tarefas.tasks.dto.response.TaskResponse;
import dev.ruancmm.gerenciador_tarefas.users.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@RequestBody TaskCreateRequest request, @AuthenticationPrincipal User user) {
        TaskResponse task = taskService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @GetMapping
    public Page<TaskResponse> list(TaskFilterRequest filter, Pageable pageable, @AuthenticationPrincipal User user) {
        return taskService.list(filter, pageable, user.getId());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TaskResponse> update(
        @PathVariable Long id, 
        @RequestBody @Valid TaskUpdateRequest request, 
        @AuthenticationPrincipal User user
    ) {
        TaskResponse task = taskService.update(request, id, user);
        return ResponseEntity.ok(task);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        taskService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trash")
    public Page<TaskResponse> trash(Pageable pageable, @AuthenticationPrincipal User user) {
        return taskService.listDeleted(pageable, user.getId());
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<TaskResponse> restore(@PathVariable Long id, @AuthenticationPrincipal User user) {
        TaskResponse task = taskService.restore(id, user);
        return ResponseEntity.ok(task);
    }

    @DeleteMapping("/trash")
    public ResponseEntity<?> emptyTrash(@AuthenticationPrincipal User user) {
        taskService.emptyTrash(user.getId());
        return ResponseEntity.noContent().build();
    }
}
