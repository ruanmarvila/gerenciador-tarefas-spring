package dev.ruancmm.gerenciador_tarefas.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Email(message = "Invalid e-mail")
    String email,

    @NotBlank(message = "Password must not be blank")
    String password
) {
}
