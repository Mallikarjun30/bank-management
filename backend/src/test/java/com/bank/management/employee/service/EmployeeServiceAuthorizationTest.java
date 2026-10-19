package com.bank.management.employee.service;

import com.bank.management.common.exception.BusinessRuleException;
import com.bank.management.employee.dto.EmployeeAssignmentRequest;
import com.bank.management.employee.dto.EmployeeStatusRequest;
import com.bank.management.branch.entity.Branch;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import com.bank.management.branch.repository.BranchRepository;
import com.bank.management.common.security.CurrentEmployee;
import com.bank.management.employee.dto.EmployeeResponse;
import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeRole;
import com.bank.management.employee.enums.EmployeeStatus;
import com.bank.management.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceAuthorizationTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private CurrentEmployee currentEmployee;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee actor;

    @BeforeEach
    void setUp() {
        actor = mock(Employee.class);
        when(currentEmployee.get()).thenReturn(actor);
        when(actor.getStatus()).thenReturn(EmployeeStatus.ACTIVE);
    }

    @Test
    void staffCannotCreateAnotherEmployee() {
        when(actor.getRole()).thenReturn(EmployeeRole.STAFF);

        assertThrows(
                AccessDeniedException.class,
                () -> employeeService.createStaff(null)
        );
    }

    @Test
    void managerCannotViewStaffAssignedToAnotherManager() {
        Employee otherManager = mock(Employee.class);
        Employee targetStaff = mock(Employee.class);

        when(actor.getId()).thenReturn(10L);
        when(actor.getRole()).thenReturn(EmployeeRole.MANAGER);
        when(targetStaff.getId()).thenReturn(100L);
        when(targetStaff.getRole()).thenReturn(EmployeeRole.STAFF);
        when(targetStaff.getReportingManager()).thenReturn(otherManager);
        when(otherManager.getId()).thenReturn(20L);
        when(employeeRepository.findById(100L))
                .thenReturn(Optional.of(targetStaff));

        assertThrows(
                AccessDeniedException.class,
                () -> employeeService.getEmployee(100L)
        );
    }

    @Test
    void staffCanViewOwnRecordButCannotSeeSalary() {
        when(actor.getId()).thenReturn(7L);
        when(actor.getRole()).thenReturn(EmployeeRole.STAFF);
        when(employeeRepository.findById(7L))
                .thenReturn(Optional.of(actor));

        EmployeeResponse response = employeeService.getEmployee(7L);

        assertEquals(7L, response.id());
        assertNull(response.monthlySalary());
    }

    @Test
void managerCannotChangeEmployeeAssignment() {
    when(actor.getRole()).thenReturn(EmployeeRole.MANAGER);

    assertThrows(
            AccessDeniedException.class,
            () -> employeeService.updateAssignment(
                    100L,
                    new EmployeeAssignmentRequest(1L, 10L)
            )
    );
}

@Test
void ownerCannotChangeOwnAssignment() {
    when(actor.getRole()).thenReturn(EmployeeRole.OWNER);

    when(employeeRepository.findById(1L))
            .thenReturn(Optional.of(actor));

    assertThrows(
            BusinessRuleException.class,
            () -> employeeService.updateAssignment(
                    1L,
                    new EmployeeAssignmentRequest(2L, null)
            )
    );

    verify(employeeRepository, never()).save(actor);
}

@Test
void managerCannotBeDeactivatedWhileActiveStaffReportToThem() {
    when(actor.getRole()).thenReturn(EmployeeRole.OWNER);

    Employee manager = mock(Employee.class);
    when(manager.getId()).thenReturn(2L);
    when(manager.getRole()).thenReturn(EmployeeRole.MANAGER);

    when(employeeRepository.findById(2L))
            .thenReturn(Optional.of(manager));

    when(employeeRepository
            .existsByReportingManager_IdAndRoleAndStatus(
                    2L,
                    EmployeeRole.STAFF,
                    EmployeeStatus.ACTIVE
            )).thenReturn(true);

    assertThrows(
            BusinessRuleException.class,
            () -> employeeService.updateStatus(
                    2L,
                    new EmployeeStatusRequest(EmployeeStatus.INACTIVE)
            )
    );

    verify(employeeRepository, never()).save(manager);
}

@Test
void ownerCanMoveManagerWithoutReassigningStaff() {
    when(actor.getRole()).thenReturn(EmployeeRole.OWNER);

    Employee manager = mock(Employee.class);
    Branch newBranch = mock(Branch.class);
    when(manager.getReportingManager()).thenReturn(actor);

    when(employeeRepository.findById(2L))
            .thenReturn(Optional.of(manager));
    when(manager.getRole()).thenReturn(EmployeeRole.MANAGER);
    when(branchRepository.findById(2L))
            .thenReturn(Optional.of(newBranch));
        
    when(employeeRepository.save(manager)).thenReturn(manager);
    when(manager.getId()).thenReturn(2L);
    when(manager.getEmployeeId()).thenReturn("EMP-MANAGER-002");        

    employeeService.updateAssignment(
            2L,
            new EmployeeAssignmentRequest(2L, null)
    );

    verify(manager).setBranch(newBranch);
    verify(employeeRepository).save(manager);

    // Moving a Manager must not trigger automatic Staff reassignment.
    verify(
            employeeRepository,
            never()
    ).findAllByReportingManager_IdAndRoleOrderByIdAsc(
            2L,
            EmployeeRole.STAFF
    );
}

@Test
void ownerCanReassignStaffToAnotherManagerInSameBranch() {
    when(actor.getRole()).thenReturn(EmployeeRole.OWNER);

    Employee staff = mock(Employee.class);
    Employee manager = mock(Employee.class);
    Branch branch = mock(Branch.class);
    when(staff.getReportingManager()).thenReturn(manager);

    when(employeeRepository.findById(100L))
            .thenReturn(Optional.of(staff));
    when(staff.getRole()).thenReturn(EmployeeRole.STAFF);

    when(branchRepository.findById(2L))
            .thenReturn(Optional.of(branch));
    when(branch.getId()).thenReturn(2L);

    when(employeeRepository.findById(20L))
            .thenReturn(Optional.of(manager));
    when(manager.getRole()).thenReturn(EmployeeRole.MANAGER);
    when(manager.getStatus()).thenReturn(EmployeeStatus.ACTIVE);
    when(manager.getBranch()).thenReturn(branch);

    when(employeeRepository.save(staff)).thenReturn(staff);
    when(staff.getId()).thenReturn(100L);
    when(staff.getEmployeeId()).thenReturn("EMP-STAFF-100");

    employeeService.updateAssignment(
            100L,
            new EmployeeAssignmentRequest(2L, 20L)
    );

    verify(staff).setBranch(branch);
    verify(staff).setReportingManager(manager);
    verify(employeeRepository).save(staff);
}
}
