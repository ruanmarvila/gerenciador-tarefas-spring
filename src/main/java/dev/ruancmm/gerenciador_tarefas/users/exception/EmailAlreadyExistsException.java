package dev.ruancmm.gerenciador_tarefas.users.exception;

import org.springframework.http.HttpStatus;

public final class EmailAlreadyExistsException extends UserException {

	public EmailAlreadyExistsException() {
		super("A user with this email already exists", HttpStatus.CONFLICT);
	}

}
