package dev.ruancmm.gerenciador_tarefas.tasks.dto.request;

public record TaskCreateRequest(
    String title,
    String description
) {
}
