package com.bank.management.employee.entity;

import com.bank.management.branch.entity.Branch;
import com.bank.management.employee.enums.EmployeeRole;
import com.bank.management.employee.enums.EmployeeStatus;
import com.bank.management.employee.enums.Gender;
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
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "employees",
        indexes = {
                @Index(name = "idx_employee_branch", columnList = "branch_id"),
                @Index(name = "idx_employee_reporting_manager", columnList = "reporting_manager_id")
        }
)
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Owner authenticates using email.
     * Manager/Staff authenticate using employeeId.
     *
     * employeeId is therefore nullable for the Owner.
     */
    @Size(max = 30)
    @Column(name = "employee_id", unique = true, length = 30)
    private String employeeId;

    /*
     * Email is required for Owner authentication.
     * It may also be stored for Manager/Staff.
     */
    @Email
    @Size(max = 255)
    @Column(name = "email", length = 255)
    private String email;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @NotBlank
    @Size(max = 150)
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Past
    @NotNull
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    /*
     * Stored as String rather than a numeric SQL type so leading zeros
     * are preserved. Validation requires digits only.
     */
    @NotBlank
    @Pattern(regexp = "\\d+", message = "Identity proof number must contain digits only")
    @Size(max = 50)
    @Column(name = "identity_proof_number", unique = true, nullable = false, length = 50)
    private String identityProofNumber;

    @NotBlank
    @Pattern(
            regexp = "^[0-9+()\\- ]{10,20}$",
            message = "Invalid phone number format"
    )
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 30)
    private Gender gender;

    @NotNull
    @Column(name = "date_of_joining", nullable = false)
    private LocalDate dateOfJoining;

    @NotNull
    @DecimalMin(value = "0.00")
    @Column(
            name = "monthly_salary",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal monthlySalary;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private EmployeeRole role;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmployeeStatus status;

    /*
     * Owner is organization-wide and therefore may have no branch.
     * Manager/Staff branch assignment is enforced by the service layer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "branch_id",
            foreignKey = @ForeignKey(name = "fk_employee_branch")
    )
    private Branch branch;

    /*
     * Self-referencing relationship:
     *
     * Owner  -> null
     * Manager -> Owner
     * Staff -> Manager
     *
     * The role-specific rules are business rules and will be enforced
     * in the service layer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reporting_manager_id",
            foreignKey = @ForeignKey(name = "fk_employee_reporting_manager")
    )
    private Employee reportingManager;

    protected Employee() {
        // Required by JPA
    }

    public Employee(
            String employeeId,
            String email,
            String passwordHash,
            String fullName,
            LocalDate dateOfBirth,
            String identityProofNumber,
            String phoneNumber,
            Gender gender,
            LocalDate dateOfJoining,
            BigDecimal monthlySalary,
            EmployeeRole role,
            EmployeeStatus status,
            Branch branch,
            Employee reportingManager
    ) {
        this.employeeId = employeeId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.identityProofNumber = identityProofNumber;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.dateOfJoining = dateOfJoining;
        this.monthlySalary = monthlySalary;
        this.role = role;
        this.status = status;
        this.branch = branch;
        this.reportingManager = reportingManager;
    }

    public Long getId() {
        return id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public LocalDate getDateOfJoining() {
        return dateOfJoining;
    }

    public void setDateOfJoining(LocalDate dateOfJoining) {
        this.dateOfJoining = dateOfJoining;
    }

    public BigDecimal getMonthlySalary() {
        return monthlySalary;
    }

    public void setMonthlySalary(BigDecimal monthlySalary) {
        this.monthlySalary = monthlySalary;
    }

    public EmployeeRole getRole() {
        return role;
    }

    public void setRole(EmployeeRole role) {
        this.role = role;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public void setStatus(EmployeeStatus status) {
        this.status = status;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public Employee getReportingManager() {
        return reportingManager;
    }

    public void setReportingManager(Employee reportingManager) {
        this.reportingManager = reportingManager;
    }
}