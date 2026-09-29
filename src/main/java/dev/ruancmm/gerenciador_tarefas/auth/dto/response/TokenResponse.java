package dev.ruancmm.gerenciador_tarefas.auth.dto.response;

public record TokenResponse(
    String accessToken,
    String refreshToken
) {
}
