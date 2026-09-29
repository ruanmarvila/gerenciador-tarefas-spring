package dev.ruancmm.gerenciador_tarefas.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.ruancmm.gerenciador_tarefas.auth.dto.request.LoginRequest;
import dev.ruancmm.gerenciador_tarefas.auth.dto.request.RefreshRequest;
import dev.ruancmm.gerenciador_tarefas.auth.dto.response.AccessTokenResponse;
import dev.ruancmm.gerenciador_tarefas.auth.dto.response.TokenResponse;
import dev.ruancmm.gerenciador_tarefas.users.dto.request.UserCreateRequest;
import dev.ruancmm.gerenciador_tarefas.users.dto.response.UserResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody @Valid UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request ) {
        return ResponseEntity.ok(authService.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/restore")
    public ResponseEntity<TokenResponse> restore(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.restoreAndLogin(request.email(), request.password()));
    }
}
