package dev.ruancmm.gerenciador_tarefas.auth;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.ruancmm.gerenciador_tarefas.users.UserRequest;
import dev.ruancmm.gerenciador_tarefas.users.UserResponse;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@RequestBody @Valid UserRequest dto) {
        return authService.register(dto);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> login(@RequestBody @Valid LoginDTO dto) {
        return authService.login(dto.email(), dto.password());
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> refresh(@RequestBody Map<String, String> body) {
        String newAccessToken = authService.refresh(body.get("refreshToken"));
        return Map.of("accessToken", newAccessToken);
    }
}
