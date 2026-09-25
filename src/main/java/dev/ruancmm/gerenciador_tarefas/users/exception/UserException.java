package dev.ruancmm.gerenciador_tarefas.users.exception;

import org.springframework.http.HttpStatus;

import dev.ruancmm.gerenciador_tarefas.core.exception.BusinessException;

public class UserException extends BusinessException {

	public UserException(String message, HttpStatus status) {
		super(message, status);
	}

}
