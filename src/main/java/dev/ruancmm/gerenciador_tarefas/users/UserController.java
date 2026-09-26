package dev.ruancmm.gerenciador_tarefas.users;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.ruancmm.gerenciador_tarefas.users.dto.request.UserUpdateRequest;
import dev.ruancmm.gerenciador_tarefas.users.dto.request.UserUpdatePasswordRequest;
import dev.ruancmm.gerenciador_tarefas.users.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(UserResponse.fromEntity(currentUser));
    }

    @PatchMapping("/update")
    public ResponseEntity<UserResponse> update(@RequestBody @Valid UserUpdateRequest request,@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.update(request, currentUser));
    }

    @PatchMapping("/update/password")
    public ResponseEntity<Map<String, String>> updatePassword(@RequestBody @Valid UserUpdatePasswordRequest request, @AuthenticationPrincipal User currentUser) {
        userService.updatePassword(request, currentUser);
        return ResponseEntity.ok(Map.of("message", "password successfully updated"));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@AuthenticationPrincipal User currentUser) {
        userService.delete(currentUser);
        return ResponseEntity.noContent().build();
    }

    
}
