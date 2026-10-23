package com.bank.management.customer.service;

import com.bank.management.branch.entity.Branch;
import com.bank.management.branch.repository.BranchRepository;
import com.bank.management.common.security.CurrentEmployee;
import com.bank.management.common.security.ResourceAuthorizationService;
import com.bank.management.customer.entity.Customer;
import com.bank.management.customer.repository.CustomerRepository;
import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private CurrentEmployee currentEmployee;

    @Mock
    private ResourceAuthorizationService authorizationService;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void ownerSearchesAcrossAllBranches() {
        Employee owner = employeeWithRole(EmployeeRole.OWNER);
        when(currentEmployee.get()).thenReturn(owner);

        when(customerRepository.searchCustomers(
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        Page<?> result = customerService.searchCustomers(null, 0, 20);

        assertEquals(0, result.getTotalElements());

        verify(customerRepository).searchCustomers(
                isNull(),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    void managerSearchIsRestrictedToAssignedBranch() {
        Employee manager = employeeInBranch(EmployeeRole.MANAGER, 1L);
        when(currentEmployee.get()).thenReturn(manager);

        when(customerRepository.searchCustomers(
                eq(1L),
                eq("smith"),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        Page<?> result = customerService.searchCustomers(" smith ", 0, 20);

        assertEquals(0, result.getTotalElements());

        verify(customerRepository).searchCustomers(
                eq(1L),
                eq("smith"),
                any(Pageable.class)
        );
    }

    @Test
    void staffSearchIsRestrictedToAssignedBranch() {
        Employee staff = employeeInBranch(EmployeeRole.STAFF, 1L);
        when(currentEmployee.get()).thenReturn(staff);

        when(customerRepository.searchCustomers(
                eq(1L),
                isNull(),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        Page<?> result = customerService.searchCustomers(null, 0, 20);

        assertEquals(0, result.getTotalElements());

        verify(customerRepository).searchCustomers(
                eq(1L),
                isNull(),
                any(Pageable.class)
        );
    }

    private Employee employeeWithRole(EmployeeRole role) {
        Employee employee = org.mockito.Mockito.mock(Employee.class);
        when(employee.getRole()).thenReturn(role);
        return employee;
    }

    private Employee employeeInBranch(EmployeeRole role, Long branchId) {
        Employee employee = employeeWithRole(role);
        Branch branch = org.mockito.Mockito.mock(Branch.class);

        when(branch.getId()).thenReturn(branchId);
        when(employee.getBranch()).thenReturn(branch);

        return employee;
    }
}
