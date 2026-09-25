package dev.ruancmm.gerenciador_tarefas.tasks.exception;

import org.springframework.http.HttpStatus;

public class TaskNotFoundException extends TaskException {

    public TaskNotFoundException() {
        super("Task not found", HttpStatus.NOT_FOUND);
    }

}
