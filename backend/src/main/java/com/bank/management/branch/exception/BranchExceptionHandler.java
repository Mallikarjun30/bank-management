package com.bank.management.branch.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.bank.management.branch")
public class BranchExceptionHandler {

    @ExceptionHandler(BranchConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(
            BranchConflictException exception
    ) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorBody(exception.getMessage()));
    }

    @ExceptionHandler(BranchNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            BranchNotFoundException exception
    ) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorBody(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException exception
    ) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error ->
                        error.getField()
                                + ": "
                                + error.getDefaultMessage()
                )
                .orElse("Invalid branch data");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorBody(message));
    }

    private Map<String, String> errorBody(String message) {

        Map<String, String> body = new LinkedHashMap<>();

        body.put("error", message);

        return body;
    }
}