package com.mhf.transaction.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Void> handleAccountNotFound(AccountNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Void> handleInsufficientBalance(InsufficientBalanceException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<Void> handleInvalidTransfer(InvalidTransferException exception) {
        return ResponseEntity.badRequest().build();
    }

}
