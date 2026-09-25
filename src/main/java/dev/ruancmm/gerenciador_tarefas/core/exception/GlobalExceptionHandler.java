package dev.ruancmm.gerenciador_tarefas.core.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handlerBusinessException(BusinessException ex) {
        ErrorResponse exception = new ErrorResponse(
            LocalDateTime.now(), ex.getStatus().value(), ex.getMessage()
        );
        return ResponseEntity.status(ex.getStatus()).body(exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handlerValidationException(MethodArgumentNotValidException ex) {
        ErrorResponse exception = new ErrorResponse(
            LocalDateTime.now(), HttpStatus.UNPROCESSABLE_CONTENT.value(), ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(exception);
    }
}
