package dev.ruancmm.gerenciador_tarefas.users.exception;

import org.springframework.http.HttpStatus;

public class PasswordReuseException extends UserException {

	public PasswordReuseException() {
		super("New password cannot be the same as the current password", HttpStatus.BAD_REQUEST);
	}

}
