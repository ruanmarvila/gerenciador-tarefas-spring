package dev.ruancmm.gerenciador_tarefas.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "E-mail must not be blank")
    @Email(message = "Invalid e-mail")
    String email,

    @NotBlank(message = "Password must not be blank")
    @Size(min = 5, message = "Password must contain at least 5 characters")
    String password
) {
}
