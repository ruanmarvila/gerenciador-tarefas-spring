package dev.ruancmm.gerenciador_tarefas.tasks.dto.response;

import dev.ruancmm.gerenciador_tarefas.tasks.Task;
import dev.ruancmm.gerenciador_tarefas.tasks.TaskStatus;

public record TaskResponse(
    Long id,
    String title,
    String description,
    TaskStatus status
) {

    public static TaskResponse fromEntity(Task task) {
        return new TaskResponse(
            task.getId(), task.getTitle(), task.getDescription(), task.getStatus()
        );
    }
}
