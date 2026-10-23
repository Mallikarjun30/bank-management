package com.bank.management.customer.entity;

import com.bank.management.branch.entity.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotNull
    @Past
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @NotBlank
    @Pattern(regexp = "[0-9]+",
            message = "Identity proof number must contain digits only")
    @Size(max = 50)
    @Column(name = "identity_proof_number",
            nullable = false, unique = true, length = 50)
    private String identityProofNumber;

    @NotBlank
    @Pattern(regexp = "^[0-9+()\\- ]{10,20}$",
            message = "Invalid phone number format")
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_customer_branch"))
    private Branch branch;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Customer() {
        // Required by JPA
    }

    public Customer(
            String name,
            LocalDate dateOfBirth,
            String identityProofNumber,
            String phoneNumber,
            Branch branch,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.identityProofNumber = identityProofNumber;
        this.phoneNumber = phoneNumber;
        this.branch = branch;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /*
     * Retained for existing unit tests and non-persisted test objects.
     * Persisted customers must have a branch.
     */
    public Customer(
            String name,
            LocalDate dateOfBirth,
            String identityProofNumber,
            String phoneNumber,
            LocalDateTime createdAt
    ) {
        this(name, dateOfBirth, identityProofNumber,
                phoneNumber, null, createdAt, createdAt);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getIdentityProofNumber() {
        return identityProofNumber;
    }

    public void setIdentityProofNumber(String identityProofNumber) {
        this.identityProofNumber = identityProofNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}