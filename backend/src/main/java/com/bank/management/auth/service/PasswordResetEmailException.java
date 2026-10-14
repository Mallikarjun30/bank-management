package com.bank.management.auth.service;

public class PasswordResetEmailException extends RuntimeException {

    public PasswordResetEmailException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}