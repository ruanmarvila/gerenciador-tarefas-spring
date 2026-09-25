package dev.ruancmm.gerenciador_tarefas.auth.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends AuthException {

	public InvalidCredentialsException() {
		super("Invalid credentials", HttpStatus.UNAUTHORIZED);
	}

}
