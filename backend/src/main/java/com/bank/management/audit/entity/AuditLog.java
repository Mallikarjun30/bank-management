package com.bank.management.audit.entity;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_actor", columnList = "actor_id"),
                @Index(name = "idx_audit_action", columnList = "action"),
                @Index(
                        name = "idx_audit_resource",
                        columnList = "entity_type, resource_id"
                ),
                @Index(name = "idx_audit_timestamp", columnList = "timestamp")
        }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Employee who performed the operation.
     *
     * This is a historical reference. An employee becoming INACTIVE
     * must not invalidate the audit record.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "actor_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_audit_actor")
    )
    private Employee actor;

    @NotBlank
    @Size(max = 100)
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /*
     * Examples:
     * EMPLOYEE
     * BRANCH
     * CUSTOMER
     * BANK_ACCOUNT
     * BANK_TRANSACTION
     * SESSION
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    /*
     * String is used because different resource types may use different
     * identifier representations.
     */
    @Size(max = 100)
    @Column(name = "resource_id", length = 100)
    private String resourceId;

    /*
     * Human-readable/contextual details about the operation.
     */
    @Size(max = 4000)
    @Column(name = "action_details", length = 4000)
    private String actionDetails;

    @NotNull
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    protected AuditLog() {
        // Required by JPA
    }

    public AuditLog(
            Employee actor,
            String action,
            String entityType,
            String resourceId,
            String actionDetails
    ) {
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.resourceId = resourceId;
        this.actionDetails = actionDetails;
    }

    @PrePersist
    private void setTimestamp() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Employee getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getActionDetails() {
        return actionDetails;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}