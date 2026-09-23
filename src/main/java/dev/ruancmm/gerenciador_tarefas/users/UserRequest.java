package dev.ruancmm.gerenciador_tarefas.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
    @NotBlank(message = "Name must not be blank")
    @Size(max = 100, message = "Name must be at most 100 characters")
    String name,

    @Email(message = "Invalid e-mail")
    String email,

    @NotBlank(message = "Password must not be blank")
    @Size(min = 5, message = "Password must contain at least 5 characters")
    String password
) {

    public User toEntity() {
        return new User(name, email, password);
    }
}
