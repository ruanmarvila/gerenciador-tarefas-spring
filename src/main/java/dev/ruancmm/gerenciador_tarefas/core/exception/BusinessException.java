package dev.ruancmm.gerenciador_tarefas.core.exception;

import org.springframework.http.HttpStatus;

public abstract class BusinessException extends RuntimeException {

    private HttpStatus status;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
