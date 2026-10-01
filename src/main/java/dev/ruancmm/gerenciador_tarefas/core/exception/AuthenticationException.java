package dev.ruancmm.gerenciador_tarefas.core.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends BusinessException {

    public AuthenticationException() {
        super("Invalid email or password", HttpStatus.UNAUTHORIZED);
    }

}
