package dev.ruancmm.gerenciador_tarefas.core.exception;

import org.springframework.http.HttpStatus;

public class AuthorizationException extends BusinessException {

    public AuthorizationException() {
        super("You don't have authorization for this operation", HttpStatus.FORBIDDEN);
    }

}
