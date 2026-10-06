package com.bank.management.branch.dto;

import com.bank.management.branch.entity.Branch;

import java.time.LocalDateTime;

public record BranchResponse(
        Long id,
        String branchId,
        String branchName,
        String location,
        LocalDateTime createdAt
) {

    public static BranchResponse from(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getBranchId(),
                branch.getBranchName(),
                branch.getLocation(),
                branch.getCreatedAt()
        );
    }
}