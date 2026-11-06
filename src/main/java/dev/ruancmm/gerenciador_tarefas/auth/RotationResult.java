package dev.ruancmm.gerenciador_tarefas.auth;

public sealed interface RotationResult {

    record Rotated(Long userId, String newRawToken) implements RotationResult {}

    record Rejected(Reason reason) implements RotationResult {
        public enum Reason {UNKNOWN, EXPIRED, REUSE_DETECTED}
    }
}
