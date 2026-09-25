package dev.ruancmm.gerenciador_tarefas.users.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends UserException {

	public AuthenticationException() {
        super("Invalid email or password", HttpStatus.UNAUTHORIZED);
	}

}
