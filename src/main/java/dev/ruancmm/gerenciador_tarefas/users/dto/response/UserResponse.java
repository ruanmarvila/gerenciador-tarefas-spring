package dev.ruancmm.gerenciador_tarefas.users.dto.response;

import java.util.List;

import dev.ruancmm.gerenciador_tarefas.tasks.Task;
import dev.ruancmm.gerenciador_tarefas.users.User;

public record UserResponse(
    Long id,
    String name,
    String email,
    List<Task> tasks
) {

    public static UserResponse fromEntity(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getTasks());
    }
}
