package com.bank.management.branch.exception;

public class BranchConflictException extends RuntimeException {

    public BranchConflictException(String message) {
        super(message);
    }
}