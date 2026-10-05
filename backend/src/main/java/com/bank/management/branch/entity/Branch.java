package com.bank.management.branch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "branches")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 20)
    @Column(name = "branch_id", nullable = false, unique = true, length = 20)
    private String branchId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "branch_name", nullable = false, length = 100)
    private String branchName;

    @NotBlank
    @Size(max = 255)
    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Branch() {
        // Required by JPA
    }

    public Branch(
            String branchId,
            String branchName,
            String location,
            LocalDateTime createdAt
    ) {
        this.branchId = branchId;
        this.branchName = branchName;
        this.location = location;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}