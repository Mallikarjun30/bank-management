package com.bank.management.auth.entity;

import com.bank.management.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "password_reset_tokens",
        indexes = {
                @Index(
                        name = "idx_password_reset_employee",
                        columnList = "employee_id"
                ),
                @Index(
                        name = "idx_password_reset_expires",
                        columnList = "expires_at"
                )
        }
)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "employee_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_password_reset_employee"
            )
    )
    private Employee employee;

    /*
     * Store only the SHA-256 hash of the raw reset token.
     * The raw token must never be persisted.
     */
    @NotNull
    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 64
    )
    private String tokenHash;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /*
     * Null until the token is successfully consumed.
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @NotNull
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    protected PasswordResetToken() {
        // Required by JPA
    }

    public PasswordResetToken(
            Employee employee,
            String tokenHash,
            LocalDateTime expiresAt
    ) {
        this.employee = employee;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    private void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void consume(LocalDateTime usedAt) {
        if (this.usedAt != null) {
            throw new IllegalStateException(
                    "Password reset token has already been used"
            );
        }

        this.usedAt = usedAt;
    }
}
