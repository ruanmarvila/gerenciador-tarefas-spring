package dev.ruancmm.gerenciador_tarefas.tasks.dto.request;

import dev.ruancmm.gerenciador_tarefas.core.validation.AtLeastOneField;
import dev.ruancmm.gerenciador_tarefas.tasks.TaskStatus;

@AtLeastOneField(fields = {"title", "description"})
public record TaskUpdateRequest(
    String title,
    String description,
    TaskStatus status
) {
}
