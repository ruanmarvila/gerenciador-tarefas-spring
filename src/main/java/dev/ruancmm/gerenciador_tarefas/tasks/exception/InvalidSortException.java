package dev.ruancmm.gerenciador_tarefas.tasks.exception;

import org.springframework.http.HttpStatus;

public final class InvalidSortException extends TaskException {

    public InvalidSortException(String property) {
        super("Invalid sort property: " + property, HttpStatus.BAD_REQUEST);
    }

}
