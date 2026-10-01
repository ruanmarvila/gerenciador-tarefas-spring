package dev.ruancmm.gerenciador_tarefas.tasks.exception;

import org.springframework.http.HttpStatus;

import dev.ruancmm.gerenciador_tarefas.core.exception.BusinessException;

public sealed class TaskException extends BusinessException permits TaskNotFoundException {

    public TaskException(String message, HttpStatus status) {
        super(message, status);
    }

}
