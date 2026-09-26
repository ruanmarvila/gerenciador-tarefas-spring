package dev.ruancmm.gerenciador_tarefas.auth.exception;

import org.springframework.http.HttpStatus;

public class AccountDisabledException extends AuthException {

    public AccountDisabledException() {
        super("Account disabled. Please reactivate your account", HttpStatus.FORBIDDEN);
    }

}
