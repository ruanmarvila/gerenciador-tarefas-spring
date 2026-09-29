package dev.ruancmm.gerenciador_tarefas.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
    @NotBlank(message = "Refresh token must not be blank")
    String refreshToken
) {
}
