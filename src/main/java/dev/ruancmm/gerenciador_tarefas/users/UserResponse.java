package dev.ruancmm.gerenciador_tarefas.users;

public record UserResponse(
    Long id,
    String name,
    String email
) {

    public static UserResponse fromEntity(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
