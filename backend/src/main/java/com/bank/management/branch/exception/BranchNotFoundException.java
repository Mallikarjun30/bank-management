package com.bank.management.branch.exception;

public class BranchNotFoundException extends RuntimeException {

    public BranchNotFoundException(Long branchId) {
        super("Branch not found: " + branchId);
    }
}