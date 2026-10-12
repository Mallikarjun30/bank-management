package com.bank.management.common.security;

/**
 * Contract for an authenticated Spring Security principal
 * that identifies a bank employee by the internal database ID.
 */
public interface EmployeePrincipal {

    Long getInternalEmployeeId();
}