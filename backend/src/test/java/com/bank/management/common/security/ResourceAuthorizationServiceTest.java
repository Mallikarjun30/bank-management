package com.bank.management.common.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bank.management.account.entity.BankAccount;
import com.bank.management.account.repository.BankAccountRepository;
import com.bank.management.branch.entity.Branch;
import com.bank.management.customer.entity.Customer;
import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeRole;

class ResourceAuthorizationServiceTest {

    private BankAccountRepository bankAccountRepository;
    private ResourceAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        bankAccountRepository = mock(BankAccountRepository.class);
        authorizationService =
                new ResourceAuthorizationService(bankAccountRepository);
    }

    @Test
    void ownerCanAccessEmployeeInAnotherBranch() {
        Employee owner = employee(1L, EmployeeRole.OWNER, null, null);
        Employee staff = employee(2L, EmployeeRole.STAFF, null, null);

        assertTrue(authorizationService.canAccessEmployee(owner, staff));
    }

    @Test
    void managerCanAccessAssignedStaff() {
        Employee manager = employee(10L, EmployeeRole.MANAGER, null, null);
        Employee staff = employee(20L, EmployeeRole.STAFF, null, manager);

        assertTrue(authorizationService.canAccessEmployee(manager, staff));
    }

    @Test
    void managerCannotAccessAnotherManagersStaff() {
        Employee manager = employee(10L, EmployeeRole.MANAGER, null, null);
        Employee otherManager =
                employee(11L, EmployeeRole.MANAGER, null, null);
        Employee staff =
                employee(20L, EmployeeRole.STAFF, null, otherManager);

        assertFalse(authorizationService.canAccessEmployee(manager, staff));
    }

    @Test
    void staffCanAccessOwnEmployeeRecord() {
        Employee staff = employee(20L, EmployeeRole.STAFF, null, null);

        assertTrue(authorizationService.canAccessEmployee(staff, staff));
    }

    @Test
    void staffCannotAccessAnotherEmployeesRecord() {
        Employee staff = employee(20L, EmployeeRole.STAFF, null, null);
        Employee otherStaff = employee(21L, EmployeeRole.STAFF, null, null);

        assertFalse(authorizationService.canAccessEmployee(staff, otherStaff));
    }

    @Test
    void ownerCannotAccessOwnerRecordThroughEmployeeCrud() {
        Employee owner = employee(1L, EmployeeRole.OWNER, null, null);

        assertFalse(authorizationService.canAccessEmployee(owner, owner));
    }

    @Test
    void nonOwnerCannotAccessDifferentBranch() {
        Branch ownBranch = branch(100L);
        Branch otherBranch = branch(200L);
        Employee manager =
                employee(10L, EmployeeRole.MANAGER, ownBranch, null);

        assertFalse(
                authorizationService.canAccessBranch(manager, otherBranch));
    }

    @Test
    void ownerCanAccessAnyBranch() {
        Employee owner = employee(1L, EmployeeRole.OWNER, null, null);
        Branch otherBranch = branch(200L);

        assertTrue(
                authorizationService.canAccessBranch(owner, otherBranch));
    }

    @Test
    void managerCannotAccessAccountInDifferentBranch() {
        Branch ownBranch = branch(100L);
        Branch otherBranch = branch(200L);
        Employee manager =
                employee(10L, EmployeeRole.MANAGER, ownBranch, null);
        BankAccount account = mock(BankAccount.class);

        when(account.getBranch()).thenReturn(otherBranch);

        assertFalse(
                authorizationService.canAccessAccount(manager, account));
    }

    @Test
    void managerCanAccessAccountInOwnBranch() {
        Branch ownBranch = branch(100L);
        Employee manager =
                employee(10L, EmployeeRole.MANAGER, ownBranch, null);
        BankAccount account = mock(BankAccount.class);

        when(account.getBranch()).thenReturn(ownBranch);

        assertTrue(
                authorizationService.canAccessAccount(manager, account));
    }

        @Test
    void ownerCanAccessCustomerWithoutAccount() {
        Employee owner = employee(1L, EmployeeRole.OWNER, null, null);
        Customer customer = customer(50L, branch(100L));

        assertTrue(authorizationService.canAccessCustomer(owner, customer));

        verifyNoInteractions(bankAccountRepository);
    }

    @Test
    void managerCanAccessCustomerWithoutAccountInOwnBranch() {
        Branch ownBranch = branch(100L);
        Employee manager =
                employee(10L, EmployeeRole.MANAGER, ownBranch, null);
        Customer customer = customer(50L, ownBranch);

        assertTrue(authorizationService.canAccessCustomer(manager, customer));

        verifyNoInteractions(bankAccountRepository);
    }

    @Test
    void managerCanAccessCustomerWithAccountInOwnBranch() {
        Branch ownBranch = branch(100L);
        Employee manager =
                employee(10L, EmployeeRole.MANAGER, ownBranch, null);
        Customer customer = customer(50L, ownBranch);

        assertTrue(authorizationService.canAccessCustomer(manager, customer));

        verifyNoInteractions(bankAccountRepository);
    }

    @Test
    void managerCannotAccessCustomerInAnotherBranch() {
        Branch ownBranch = branch(100L);
        Branch otherBranch = branch(200L);
        Employee manager =
                employee(10L, EmployeeRole.MANAGER, ownBranch, null);
        Customer customer = customer(50L, otherBranch);

        assertFalse(authorizationService.canAccessCustomer(manager, customer));

        verifyNoInteractions(bankAccountRepository);
    }

    @Test
    void staffCanAccessCustomerInOwnBranch() {
        Branch ownBranch = branch(100L);
        Employee staff =
                employee(20L, EmployeeRole.STAFF, ownBranch, null);
        Customer customer = customer(50L, ownBranch);

        assertTrue(authorizationService.canAccessCustomer(staff, customer));
    }

    @Test
    void staffCannotAccessCustomerInAnotherBranch() {
        Branch ownBranch = branch(100L);
        Branch otherBranch = branch(200L);
        Employee staff =
                employee(20L, EmployeeRole.STAFF, ownBranch, null);
        Customer customer = customer(50L, otherBranch);

        assertFalse(authorizationService.canAccessCustomer(staff, customer));
    }

    @Test
    void nonOwnerCannotAccessCustomerWithoutBranch() {
        Employee manager =
                employee(10L, EmployeeRole.MANAGER, branch(100L), null);
        Customer customer = customer(50L);

        assertFalse(authorizationService.canAccessCustomer(manager, customer));
    }

    @Test
    void nullEmployeeResourcesAreDenied() {
        Employee staff = employee(20L, EmployeeRole.STAFF, null, null);
        Branch branch = branch(100L);
        BankAccount account = mock(BankAccount.class);
        Customer customer = customer(50L);

        assertFalse(authorizationService.canAccessEmployee(null, staff));
        assertFalse(authorizationService.canAccessEmployee(staff, null));
        assertFalse(authorizationService.canAccessBranch(null, branch));
        assertFalse(authorizationService.canAccessBranch(staff, null));
        assertFalse(authorizationService.canAccessAccount(null, account));
        assertFalse(authorizationService.canAccessAccount(staff, null));
        assertFalse(authorizationService.canAccessCustomer(null, customer));
        assertFalse(authorizationService.canAccessCustomer(staff, null));
    }

    @Test
void ownerCanManageManager() {
    Employee owner = employee(1L, EmployeeRole.OWNER, null, null);
    Employee manager = employee(10L, EmployeeRole.MANAGER, null, owner);

    assertTrue(authorizationService.canManageEmployee(owner, manager));
}

@Test
void managerCanManageAssignedStaff() {
    Employee manager = employee(10L, EmployeeRole.MANAGER, null, null);
    Employee staff = employee(20L, EmployeeRole.STAFF, null, manager);

    assertTrue(authorizationService.canManageEmployee(manager, staff));
}

@Test
void managerCannotManageAnotherManagersStaff() {
    Employee manager = employee(10L, EmployeeRole.MANAGER, null, null);
    Employee otherManager = employee(11L, EmployeeRole.MANAGER, null, null);
    Employee staff = employee(20L, EmployeeRole.STAFF, null, otherManager);

    assertFalse(authorizationService.canManageEmployee(manager, staff));
}

@Test
void staffCannotManageAnotherEmployee() {
    Employee staff = employee(20L, EmployeeRole.STAFF, null, null);
    Employee otherStaff = employee(21L, EmployeeRole.STAFF, null, null);

    assertFalse(authorizationService.canManageEmployee(staff, otherStaff));
}

@Test
void nobodyCanManageOwnerRecordThroughEmployeeCrud() {
    Employee owner = employee(1L, EmployeeRole.OWNER, null, null);
    Employee manager = employee(10L, EmployeeRole.MANAGER, null, owner);

    assertFalse(authorizationService.canManageEmployee(owner, owner));
    assertFalse(authorizationService.canManageEmployee(manager, owner));
}

@Test
void nullEmployeeCannotManageResources() {
    Employee staff = employee(20L, EmployeeRole.STAFF, null, null);

    assertFalse(authorizationService.canManageEmployee(null, staff));
    assertFalse(authorizationService.canManageEmployee(staff, null));
}

    private Employee employee(
            Long id,
            EmployeeRole role,
            Branch branch,
            Employee reportingManager) {

        Employee employee = mock(Employee.class);

        when(employee.getId()).thenReturn(id);
        when(employee.getRole()).thenReturn(role);
        when(employee.getBranch()).thenReturn(branch);
        when(employee.getReportingManager()).thenReturn(reportingManager);

        return employee;
    }

    private Branch branch(Long id) {
        Branch branch = mock(Branch.class);
        when(branch.getId()).thenReturn(id);
        return branch;
    }

    private Customer customer(Long id) {
        return customer(id, null);
    }

    private Customer customer(Long id, Branch branch) {
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(id);
        when(customer.getBranch()).thenReturn(branch);
        return customer;
    }
}