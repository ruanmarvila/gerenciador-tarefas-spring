package dev.ruancmm.gerenciador_tarefas.auth.exception;

import org.springframework.http.HttpStatus;

public class AccountAlreadyActivateException extends AuthException {

    public AccountAlreadyActivateException() {
        super("Account is already activate", HttpStatus.CONFLICT);
    }

}
