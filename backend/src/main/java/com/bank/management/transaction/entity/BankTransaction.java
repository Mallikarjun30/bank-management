package com.bank.management.transaction.entity;

import com.bank.management.account.entity.BankAccount;
import com.bank.management.branch.entity.Branch;
import com.bank.management.employee.entity.Employee;
import com.bank.management.transaction.enums.TransactionStatus;
import com.bank.management.transaction.enums.TransactionType;
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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "bank_transactions",
        indexes = {
                @Index(name = "idx_transaction_account", columnList = "account_id"),
                @Index(name = "idx_transaction_performed_by", columnList = "performed_by"),
                @Index(name = "idx_transaction_branch", columnList = "branch_id"),
                @Index(name = "idx_transaction_type", columnList = "type")
        }
)
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Public/business transaction identifier.
     *
     * The database primary key remains Long, while transactionId gives
     * us a stable unique identifier suitable for API responses and
     * future external references.
     */
    @Column(
            name = "transaction_id",
            nullable = false,
            unique = true,
            updatable = false,
            length = 36
    )
    private String transactionId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "account_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_transaction_account")
    )
    private BankAccount account;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @NotNull
    @DecimalMin(
            value = "0.01",
            message = "Transaction amount must be greater than zero"
    )
    @Column(
            name = "amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    /*
     * Snapshot of the account balance immediately before the
     * transaction.
     */
    @NotNull
    @DecimalMin(value = "0.00")
    @Column(
            name = "previous_balance",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal previousBalance;

    /*
     * Snapshot of the account balance immediately after the
     * transaction.
     */
    @NotNull
    @DecimalMin(value = "0.00")
    @Column(
            name = "new_balance",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal newBalance;

    @NotNull
    @Column(name = "transaction_datetime", nullable = false)
    private LocalDateTime transactionDateTime;

    /*
     * Historical reference to the employee who performed the operation.
     *
     * No cascade delete is configured, so deactivating an employee does
     * not remove historical transaction records.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "performed_by",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_transaction_employee")
    )
    private Employee performedBy;

    /*
     * Branch is stored explicitly instead of deriving it only through
     * account -> branch. This preserves the branch context of the
     * transaction as required by the data model.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "branch_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_transaction_branch")
    )
    private Branch branch;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Size(max = 500)
    @Column(name = "reference", length = 500)
    private String reference;

    protected BankTransaction() {
        // Required by JPA
    }

    public BankTransaction(
            BankAccount account,
            TransactionType type,
            BigDecimal amount,
            BigDecimal previousBalance,
            BigDecimal newBalance,
            LocalDateTime transactionDateTime,
            Employee performedBy,
            Branch branch,
            TransactionStatus status,
            String reference
    ) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.transactionDateTime = transactionDateTime;
        this.performedBy = performedBy;
        this.branch = branch;
        this.status = status;
        this.reference = reference;
    }

    @PrePersist
    private void generateTransactionId() {
        if (transactionId == null) {
            transactionId = UUID.randomUUID().toString();
        }
    }

    public Long getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public BankAccount getAccount() {
        return account;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPreviousBalance() {
        return previousBalance;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    public LocalDateTime getTransactionDateTime() {
        return transactionDateTime;
    }

    public Employee getPerformedBy() {
        return performedBy;
    }

    public Branch getBranch() {
        return branch;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getReference() {
        return reference;
    }
}