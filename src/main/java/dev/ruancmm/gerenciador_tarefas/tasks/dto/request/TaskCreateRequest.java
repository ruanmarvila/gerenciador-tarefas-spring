package dev.ruancmm.gerenciador_tarefas.tasks.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TaskCreateRequest(
    @NotBlank(message = "Title must not be blank")
    String title,

    @NotBlank(message = "Description must not be blank")
    String description
) {
}
