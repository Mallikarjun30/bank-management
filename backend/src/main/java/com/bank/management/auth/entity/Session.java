package com.bank.management.auth.entity;

import com.bank.management.auth.enums.SessionStatus;
import com.bank.management.auth.enums.SessionTerminationReason;
import com.bank.management.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "sessions",
        indexes = {
                @Index(name = "idx_session_employee", columnList = "employee_id"),
                @Index(name = "idx_session_status", columnList = "status")
        }
)
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "session_id",
            nullable = false,
            unique = true,
            updatable = false,
            length = 36
    )
    private String sessionId;

    /*
     * The employee remains referenced historically even after the
     * employee is deactivated.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "employee_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_session_employee")
    )
    private Employee employee;

    @NotNull
    @Column(name = "login_at", nullable = false, updatable = false)
    private LocalDateTime loginAt;

    /*
     * Null while the session is active.
     * Set when the session logs out, expires or is revoked.
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "termination_reason", length = 20)
    private SessionTerminationReason terminationReason;

    /*
     * Store only the hash of the refresh token.
     * The actual refresh token is never persisted.
     */
    @Size(max = 255)
    @Column(name = "refresh_token_hash", length = 255)
    private String refreshTokenHash;

    @Column(name = "refresh_token_expires_at")
    private LocalDateTime refreshTokenExpiresAt;

    @Column(name = "refresh_token_revoked_at")
    private LocalDateTime refreshTokenRevokedAt;

    protected Session() {
        // Required by JPA
    }

    public Session(
            Employee employee,
            LocalDateTime loginAt,
            String refreshTokenHash,
            LocalDateTime refreshTokenExpiresAt
    ) {
        this.employee = employee;
        this.loginAt = loginAt;
        this.refreshTokenHash = refreshTokenHash;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.status = SessionStatus.ACTIVE;
    }

    public SessionStatus getStatus() {
    return status;
}

    @PrePersist
    private void generateSessionId() {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }
    }

    public Long getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LocalDateTime getLoginAt() {
        return loginAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void endSession(
            LocalDateTime endedAt,
            SessionStatus status,
            SessionTerminationReason terminationReason
    ) {
        this.endedAt = endedAt;
        this.status = status;
        this.terminationReason = terminationReason;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public LocalDateTime getRefreshTokenExpiresAt() {
        return refreshTokenExpiresAt;
    }

    public LocalDateTime getRefreshTokenRevokedAt() {
        return refreshTokenRevokedAt;
    }

    public void revokeRefreshToken(LocalDateTime revokedAt) {
        this.refreshTokenRevokedAt = revokedAt;
    }
}