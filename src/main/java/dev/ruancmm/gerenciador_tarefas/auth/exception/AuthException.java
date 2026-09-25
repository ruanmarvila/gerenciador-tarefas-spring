package dev.ruancmm.gerenciador_tarefas.auth.exception;

import org.springframework.http.HttpStatus;

import dev.ruancmm.gerenciador_tarefas.core.exception.BusinessException;

public class AuthException extends BusinessException {

	public AuthException(String message, HttpStatus status) {
		super(message, status);
	}

}
