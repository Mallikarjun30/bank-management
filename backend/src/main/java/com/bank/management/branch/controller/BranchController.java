package com.bank.management.branch.controller;

import com.bank.management.branch.dto.BranchRequest;
import com.bank.management.branch.dto.BranchResponse;
import com.bank.management.branch.service.BranchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BranchResponse> createBranch(
            @Valid @RequestBody BranchRequest request
    ) {

        BranchResponse response =
                branchService.createBranch(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<BranchResponse>> getAllBranches() {

        return ResponseEntity.ok(
                branchService.getAllBranches()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BranchResponse> getBranch(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                branchService.getBranch(id)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BranchResponse> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody BranchRequest request
    ) {

        return ResponseEntity.ok(
                branchService.updateBranch(id, request)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteBranch(
            @PathVariable Long id
    ) {

        branchService.deleteBranch(id);

        return ResponseEntity.noContent().build();
    }
}