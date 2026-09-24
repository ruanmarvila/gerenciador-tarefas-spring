package dev.ruancmm.gerenciador_tarefas.users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public record UserUpdateRequest(
    @NotBlank(message = "Name must not be blank")
    String name,

    @Email(message = "Invalid e-mail")
    String email
) {
}
