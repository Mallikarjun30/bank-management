package com.bank.management.employee.service;

import com.bank.management.common.security.ResourceAuthorizationService;

import com.bank.management.branch.entity.Branch;
import com.bank.management.branch.repository.BranchRepository;
import com.bank.management.common.exception.BusinessRuleException;
import com.bank.management.common.exception.ResourceConflictException;
import com.bank.management.common.exception.ResourceNotFoundException;
import com.bank.management.common.security.CurrentEmployee;
import com.bank.management.employee.dto.CreateEmployeeRequest;
import com.bank.management.employee.dto.EmployeeAssignmentRequest;
import com.bank.management.employee.dto.EmployeeResponse;
import com.bank.management.employee.dto.EmployeeStatusRequest;
import com.bank.management.employee.dto.UpdateEmployeeRequest;
import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeRole;
import com.bank.management.employee.enums.EmployeeStatus;
import com.bank.management.employee.repository.EmployeeRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final CurrentEmployee currentEmployee;
    private final PasswordEncoder passwordEncoder;
    private final ResourceAuthorizationService resourceAuthorizationService;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            BranchRepository branchRepository,
            CurrentEmployee currentEmployee,
            PasswordEncoder passwordEncoder,
            ResourceAuthorizationService resourceAuthorizationService
    ) {
        this.employeeRepository = employeeRepository;
        this.branchRepository = branchRepository;
        this.currentEmployee = currentEmployee;
        this.passwordEncoder = passwordEncoder;
        this.resourceAuthorizationService = resourceAuthorizationService;
    }

    public EmployeeResponse createManager(CreateEmployeeRequest request) {
        Employee actor = requireActiveActor();
        requireOwner(actor);

        Branch branch = findBranch(request.branchId());
        Employee owner = employeeRepository
                .findFirstByRoleAndStatusOrderByIdAsc(
                        EmployeeRole.OWNER, EmployeeStatus.ACTIVE)
                .orElseThrow(() ->
                        new BusinessRuleException("An active Owner must exist"));

        Employee manager = buildEmployee(
                request,
                EmployeeRole.MANAGER,
                branch,
                owner
        );

        return EmployeeResponse.from(employeeRepository.save(manager), true);
    }

    public EmployeeResponse createStaff(CreateEmployeeRequest request) {
        Employee actor = requireActiveActor();

        Branch branch;
        Employee reportingManager;

        if (actor.getRole() == EmployeeRole.OWNER) {
            branch = findBranch(request.branchId());
            reportingManager = findActiveManager(request.reportingManagerId());

            if (!Objects.equals(branch.getId(),
                    reportingManager.getBranch().getId())) {
                throw new BusinessRuleException(
                        "Staff branch must match the reporting Manager's branch");
            }
        } else if (actor.getRole() == EmployeeRole.MANAGER) {
            reportingManager = actor;
            branch = actor.getBranch();

            if (branch == null) {
                throw new BusinessRuleException(
                        "The Manager must be assigned to a branch");
            }

            if (request.branchId() != null
                    && !Objects.equals(request.branchId(), branch.getId())) {
                throw new BusinessRuleException(
                        "Managers can create Staff only in their own branch");
            }

            if (request.reportingManagerId() != null
                    && !Objects.equals(
                            request.reportingManagerId(), actor.getId())) {
                throw new BusinessRuleException(
                        "Staff must report to the creating Manager");
            }
        } else {
            throw new AccessDeniedException(
                    "Staff cannot create employees");
        }

        Employee staff = buildEmployee(
                request,
                EmployeeRole.STAFF,
                branch,
                reportingManager
        );

        return EmployeeResponse.from(employeeRepository.save(staff), true);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getEmployees() {
        Employee actor = requireActiveActor();

        List<Employee> employees;

        if (actor.getRole() == EmployeeRole.OWNER) {
            employees = employeeRepository.findAllByRoleInOrderByIdAsc(
                    List.of(EmployeeRole.MANAGER, EmployeeRole.STAFF));
        } else if (actor.getRole() == EmployeeRole.MANAGER) {
            employees = employeeRepository
                    .findAllByReportingManager_IdAndRoleOrderByIdAsc(
                            actor.getId(), EmployeeRole.STAFF);
        } else {
            employees = List.of(actor);
        }

        return employees.stream()
                .map(employee -> EmployeeResponse.from(
                        employee, canViewSalary(actor, employee)))
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(Long id) {
        Employee actor = requireActiveActor();
        Employee target = findEmployee(id);

        authorizeView(actor, target);

        return EmployeeResponse.from(
                target, canViewSalary(actor, target));
    }

    public EmployeeResponse updateEmployee(
            Long id,
            UpdateEmployeeRequest request
    ) {
        Employee actor = requireActiveActor();
        Employee target = findEmployee(id);

        authorizeManagement(actor, target);
        validateEmployeeDates(
                request.dateOfBirth(), request.dateOfJoining());
        validateSalary(request.monthlySalary());

        String employeeId = normalize(request.employeeId());
        String identityProof = normalize(request.identityProofNumber());

        if (employeeRepository.existsByEmployeeIdAndIdNot(
                employeeId, target.getId())) {
            throw new ResourceConflictException(
                    "Employee ID already exists");
        }

        if (employeeRepository.existsByIdentityProofNumberAndIdNot(
                identityProof, target.getId())) {
            throw new ResourceConflictException(
                    "Identity proof number already exists");
        }

        target.setEmployeeId(employeeId);
        target.setEmail(normalizeOptional(request.email()));
        target.setFullName(normalize(request.fullName()));
        target.setDateOfBirth(request.dateOfBirth());
        target.setIdentityProofNumber(identityProof);
        target.setPhoneNumber(normalize(request.phoneNumber()));
        target.setGender(request.gender());
        target.setDateOfJoining(request.dateOfJoining());
        target.setMonthlySalary(request.monthlySalary());

        return EmployeeResponse.from(
                employeeRepository.save(target),
                canViewSalary(actor, target));
    }

    public EmployeeResponse updateAssignment(
            Long id,
            EmployeeAssignmentRequest request
    ) {
        Employee actor = requireActiveActor();
        requireOwner(actor);

        Employee target = findEmployee(id);

        if (target.getRole() == EmployeeRole.OWNER) {
            throw new BusinessRuleException(
                    "The Owner's assignment cannot be changed");
        }

        Branch branch = findBranch(request.branchId());

        if (target.getRole() == EmployeeRole.MANAGER) {
            target.setBranch(branch);
        } else if (target.getRole() == EmployeeRole.STAFF) {
            Employee manager = findActiveManager(
                    request.reportingManagerId());

            if (!Objects.equals(
                    branch.getId(), manager.getBranch().getId())) {
                throw new BusinessRuleException(
                        "Staff branch must match the reporting Manager's branch");
            }

            target.setBranch(branch);
            target.setReportingManager(manager);
        }

        return EmployeeResponse.from(
                employeeRepository.save(target), true);
    }

    public EmployeeResponse updateStatus(
            Long id,
            EmployeeStatusRequest request
    ) {
        Employee actor = requireActiveActor();
        Employee target = findEmployee(id);

        authorizeManagement(actor, target);

        if (target.getRole() == EmployeeRole.OWNER) {
            throw new BusinessRuleException(
                    "The Owner cannot be deactivated through employee CRUD");
        }

        if (request.status() != EmployeeStatus.INACTIVE) {
            throw new BusinessRuleException(
                    "This endpoint only supports deactivation");
        }

        if (target.getStatus() == EmployeeStatus.INACTIVE) {
            throw new BusinessRuleException(
                    "Employee is already inactive");
        }

        if (target.getRole() == EmployeeRole.MANAGER
                && employeeRepository
                .existsByReportingManager_IdAndRoleAndStatus(
                        target.getId(),
                        EmployeeRole.STAFF,
                        EmployeeStatus.ACTIVE)) {
            throw new BusinessRuleException(
                    "Reassign all active Staff before deactivating this Manager");
        }

        target.setStatus(EmployeeStatus.INACTIVE);

        return EmployeeResponse.from(
                employeeRepository.save(target), true);
    }

    private Employee buildEmployee(
            CreateEmployeeRequest request,
            EmployeeRole role,
            Branch branch,
            Employee reportingManager
    ) {
        validateEmployeeDates(
                request.dateOfBirth(), request.dateOfJoining());
        validateSalary(request.monthlySalary());

        String employeeId = normalize(request.employeeId());
        String identityProof = normalize(request.identityProofNumber());

        if (employeeRepository.existsByEmployeeId(employeeId)) {
            throw new ResourceConflictException(
                    "Employee ID already exists");
        }

        if (employeeRepository.existsByIdentityProofNumber(identityProof)) {
            throw new ResourceConflictException(
                    "Identity proof number already exists");
        }

        String email = normalizeOptional(request.email());

        return new Employee(
                employeeId,
                email,
                passwordEncoder.encode(request.initialPassword()),
                normalize(request.fullName()),
                request.dateOfBirth(),
                identityProof,
                normalize(request.phoneNumber()),
                request.gender(),
                request.dateOfJoining(),
                request.monthlySalary(),
                role,
                EmployeeStatus.ACTIVE,
                branch,
                reportingManager
        );
    }

    private Employee requireActiveActor() {
        Employee actor = currentEmployee.get();

        if (actor.getStatus() != EmployeeStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Inactive employees cannot perform this operation");
        }

        return actor;
    }

    private void authorizeView(Employee actor, Employee target) {
        if (target.getRole() == EmployeeRole.OWNER) {
            throw new BusinessRuleException(
            "Owner records are not exposed through employee CRUD");
        }

        if (!resourceAuthorizationService.canAccessEmployee(actor, target)) {
            throw new AccessDeniedException(
                "You are not allowed to view this employee");
        }
    }

    private void authorizeManagement(Employee actor, Employee target) {
        if (!resourceAuthorizationService.canManageEmployee(actor, target)) {
            throw new AccessDeniedException(
                "You are not allowed to manage this employee");
        }
    }

    private boolean canViewSalary(Employee actor, Employee target) {
        return actor.getRole() == EmployeeRole.OWNER
                || (actor.getRole() == EmployeeRole.MANAGER
                && target.getRole() == EmployeeRole.STAFF
                && target.getReportingManager() != null
                && Objects.equals(
                        target.getReportingManager().getId(), actor.getId()));
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found: " + id));
    }

    private Branch findBranch(Long id) {
        if (id == null) {
            throw new BusinessRuleException(
                    "A branch is required");
        }

        return branchRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Branch not found: " + id));
    }

    private Employee findActiveManager(Long id) {
        if (id == null) {
            throw new BusinessRuleException(
                    "A reporting Manager is required");
        }

        Employee manager = findEmployee(id);

        if (manager.getRole() != EmployeeRole.MANAGER
                || manager.getStatus() != EmployeeStatus.ACTIVE
                || manager.getBranch() == null) {
            throw new BusinessRuleException(
                    "Reporting authority must be an active Manager assigned to a branch");
        }

        return manager;
    }

    private void requireOwner(Employee actor) {
        if (actor.getRole() != EmployeeRole.OWNER) {
            throw new AccessDeniedException(
                    "Only the Owner can perform this operation");
        }
    }

    private void validateEmployeeDates(
            LocalDate dateOfBirth,
            LocalDate dateOfJoining
    ) {
        LocalDate today = LocalDate.now();

        if (dateOfBirth == null || !dateOfBirth.isBefore(today)) {
            throw new BusinessRuleException(
                    "Date of birth must be in the past");
        }

        if (dateOfJoining == null || dateOfJoining.isAfter(today)) {
            throw new BusinessRuleException(
                    "Date of joining cannot be in the future");
        }

        if (dateOfJoining.isBefore(dateOfBirth)) {
            throw new BusinessRuleException(
                    "Date of joining cannot be before date of birth");
        }
    }

    private void validateSalary(BigDecimal salary) {
        if (salary == null || salary.signum() <= 0
                || salary.scale() > 2
                || salary.precision() > 19) {
            throw new BusinessRuleException(
                    "Monthly salary must be positive and have at most two decimal places");
        }
    }

    private String normalize(String value) {
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}