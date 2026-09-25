package dev.ruancmm.gerenciador_tarefas.tasks.dto.request;

import dev.ruancmm.gerenciador_tarefas.tasks.TaskStatus;

public record TaskUpdateRequest(
    String title,
    String description,
    TaskStatus status
) {
}
