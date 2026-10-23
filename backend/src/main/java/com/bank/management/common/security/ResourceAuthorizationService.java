
package com.bank.management.common.security;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.bank.management.account.entity.BankAccount;
import com.bank.management.account.repository.BankAccountRepository;
import com.bank.management.branch.entity.Branch;
import com.bank.management.customer.entity.Customer;
import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeRole;

@Service
public class ResourceAuthorizationService {

    private final BankAccountRepository bankAccountRepository;

    public ResourceAuthorizationService(
            BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public boolean canAccessEmployee(Employee actor, Employee target) {
        if (actor == null || target == null) {
            return false;
        }

        // Employee CRUD does not expose the Owner's own record.
        if (target.getRole() == EmployeeRole.OWNER) {
            return false;
        }

        if (actor.getRole() == EmployeeRole.OWNER) {
            return true;
        }

        // Employees may access their own employee record.
        if (Objects.equals(actor.getId(), target.getId())) {
            return true;
        }

        return actor.getRole() == EmployeeRole.MANAGER
                && target.getRole() == EmployeeRole.STAFF
                && target.getReportingManager() != null
                && Objects.equals(
                        actor.getId(),
                        target.getReportingManager().getId());
    }

    public boolean canAccessBranch(Employee actor, Branch branch) {
        if (actor == null || branch == null) {
            return false;
        }

        if (actor.getRole() == EmployeeRole.OWNER) {
            return true;
        }

        return actor.getBranch() != null
                && Objects.equals(
                        actor.getBranch().getId(),
                        branch.getId());
    }

    public boolean canAccessAccount(
            Employee actor,
            BankAccount account) {
        if (actor == null || account == null) {
            return false;
        }

        if (actor.getRole() == EmployeeRole.OWNER) {
            return true;
        }

        return canAccessBranch(actor, account.getBranch());
    }

    public boolean canAccessCustomer(
            Employee actor,
            Customer customer) {
        if (actor == null || customer == null) {
            return false;
        }

        if (actor.getRole() == EmployeeRole.OWNER) {
            return true;
        }

        // Customer access is based on branch ownership, not on
        // whether the customer already has a bank account.
        return canAccessBranch(actor, customer.getBranch());
    }

    public boolean canManageEmployee(Employee actor, Employee target) {
        if (actor == null || target == null) {
            return false;
        }

        // The Owner's record is not manageable through employee CRUD.
        if (target.getRole() == EmployeeRole.OWNER) {
            return false;
        }

        // The Owner can manage all other employee records.
        if (actor.getRole() == EmployeeRole.OWNER) {
            return true;
        }

        // Managers can manage only their assigned Staff.
        return actor.getRole() == EmployeeRole.MANAGER
                && target.getRole() == EmployeeRole.STAFF
                && target.getReportingManager() != null
                && Objects.equals(
                        actor.getId(),
                        target.getReportingManager().getId());
    }

}
