package dev.ruancmm.gerenciador_tarefas.auth.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends AuthException {

    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

}
