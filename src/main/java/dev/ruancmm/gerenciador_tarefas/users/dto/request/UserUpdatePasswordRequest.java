package dev.ruancmm.gerenciador_tarefas.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdatePasswordRequest(
    @NotBlank(message = "Password must not be blank")
    @Size(min = 5, message = "Password must contain at least 5 characters")
    String password,

    @NotBlank(message = "New password must not be blank")
    @Size(min = 5, message = "New password must contain at least 5 characters")
    String newPassword
) {
}
