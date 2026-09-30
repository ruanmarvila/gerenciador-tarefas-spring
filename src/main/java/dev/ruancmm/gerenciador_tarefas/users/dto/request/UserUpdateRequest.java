package dev.ruancmm.gerenciador_tarefas.users.dto.request;

import dev.ruancmm.gerenciador_tarefas.core.validation.AtLeastOneField;
import jakarta.validation.constraints.Email;

@AtLeastOneField(fields = {"name", "email"})
public record UserUpdateRequest(
    String name,

    @Email(message = "Invalid e-mail")
    String email
) {
}
