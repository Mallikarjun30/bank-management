package com.bank.management.account.entity;

import com.bank.management.account.enums.AccountStatus;
import com.bank.management.account.enums.AccountType;
import com.bank.management.branch.entity.Branch;
import com.bank.management.customer.entity.Customer;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "bank_accounts",
        indexes = {
                @Index(name = "idx_account_branch", columnList = "branch_id"),
                @Index(name = "idx_account_created_by", columnList = "created_by")
        }
)
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Account number is a business identifier and must contain
     * exactly 9 digits.
     */
    @NotBlank
    @Pattern(
            regexp = "\\d{9}",
            message = "Account number must contain exactly 9 digits"
    )
    @Column(
            name = "account_number",
            nullable = false,
            unique = true,
            length = 9
    )
    private String accountNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    /*
     * Financial values use BigDecimal.
     *
     * New accounts begin with zero balance. The service layer will
     * enforce this when creating an account.
     */
    @NotNull
    @DecimalMin(value = "0.00")
    @Column(
            name = "balance",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal balance = BigDecimal.ZERO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    /*
     * BankAccount owns the Customer relationship.
     *
     * unique = true at the database level means one customer
     * cannot appear in two bank account rows.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_account_customer")
    )
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "branch_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_account_branch")
    )
    private Branch branch;

    /*
     * Employee who created the account.
     *
     * This reference is intentionally historical and remains valid
     * even when the employee later becomes INACTIVE.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_account_created_by")
    )
    private Employee createdBy;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected BankAccount() {
        // Required by JPA
    }

    public BankAccount(
            String accountNumber,
            AccountType accountType,
            Customer customer,
            Branch branch,
            Employee createdBy,
            LocalDateTime createdAt
    ) {
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.customer = customer;
        this.branch = branch;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.balance = BigDecimal.ZERO;
        this.status = AccountStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public Employee getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Employee createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}