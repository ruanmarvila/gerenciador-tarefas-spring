package dev.ruancmm.gerenciador_tarefas.users.dto.request;

import jakarta.validation.constraints.Email;

public record UserUpdateRequest(
    String name,

    @Email(message = "Invalid e-mail")
    String email
) {
}
