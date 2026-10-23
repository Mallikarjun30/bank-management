package com.bank.management.customer.service;

import com.bank.management.branch.entity.Branch;
import com.bank.management.branch.repository.BranchRepository;
import com.bank.management.common.exception.BusinessRuleException;
import com.bank.management.common.exception.ResourceConflictException;
import com.bank.management.common.exception.ResourceNotFoundException;
import com.bank.management.common.security.CurrentEmployee;
import com.bank.management.common.security.ResourceAuthorizationService;
import com.bank.management.customer.dto.CustomerCreateRequest;
import com.bank.management.customer.dto.CustomerResponse;
import com.bank.management.customer.dto.CustomerUpdateRequest;
import com.bank.management.customer.entity.Customer;
import com.bank.management.customer.repository.CustomerRepository;
import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeRole;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final CurrentEmployee currentEmployee;
    private final ResourceAuthorizationService authorizationService;

    public CustomerService(
            CustomerRepository customerRepository,
            BranchRepository branchRepository,
            CurrentEmployee currentEmployee,
            ResourceAuthorizationService authorizationService
    ) {
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.currentEmployee = currentEmployee;
        this.authorizationService = authorizationService;
    }

    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        Employee actor = currentEmployee.get();
        Branch branch = resolveCreationBranch(actor, request.branchId());

        String identityProof = normalize(request.identityProofNumber());

        if (customerRepository.existsByIdentityProofNumber(identityProof)) {
            throw new ResourceConflictException(
                    "A customer with this identity proof number already exists");
        }

        LocalDateTime now = LocalDateTime.now();

        Customer customer = new Customer(
                normalize(request.name()),
                request.dateOfBirth(),
                identityProof,
                normalize(request.phoneNumber()),
                branch,
                now,
                now
        );

        try {
            return CustomerResponse.from(
                    customerRepository.saveAndFlush(customer));
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceConflictException(
                    "Customer could not be saved because a unique or database constraint was violated");
        }
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomers(int page, int size) {
        return searchCustomers(null, page, size);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> searchCustomers(
            String query,
            int page,
            int size
    ) {
        validatePagination(page, size);

        Employee actor = currentEmployee.get();
        Long branchScope = getBranchScope(actor);

        String normalizedQuery =
                query == null || query.isBlank() ? null : query.trim();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "name")
        );

        return customerRepository
                .searchCustomers(branchScope, normalizedQuery, pageable)
                .map(CustomerResponse::from);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long id) {
        Employee actor = currentEmployee.get();
        Customer customer = findCustomer(id);
        verifyAccess(actor, customer);
        return CustomerResponse.from(customer);
    }

    public CustomerResponse updateCustomer(
            Long id,
            CustomerUpdateRequest request
    ) {
        Employee actor = currentEmployee.get();
        Customer customer = findCustomer(id);
        verifyAccess(actor, customer);

        String identityProof = normalize(request.identityProofNumber());

        if (customerRepository.existsByIdentityProofNumberAndIdNot(
                identityProof, id)) {
            throw new ResourceConflictException(
                    "A customer with this identity proof number already exists");
        }

        customer.setName(normalize(request.name()));
        customer.setDateOfBirth(request.dateOfBirth());
        customer.setIdentityProofNumber(identityProof);
        customer.setPhoneNumber(normalize(request.phoneNumber()));
        customer.setUpdatedAt(LocalDateTime.now());

        try {
            return CustomerResponse.from(
                    customerRepository.saveAndFlush(customer));
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceConflictException(
                    "Customer could not be updated because a unique or database constraint was violated");
        }
    }

    private Branch resolveCreationBranch(Employee actor, Long requestedBranchId) {
        if (actor.getRole() == EmployeeRole.OWNER) {
            if (requestedBranchId == null) {
                throw new BusinessRuleException(
                        "Owner must provide a branchId when creating a customer");
            }

            return branchRepository.findById(requestedBranchId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Branch not found: " + requestedBranchId));
        }

        Branch employeeBranch = actor.getBranch();

        if (employeeBranch == null) {
            throw new AccessDeniedException(
                    "Your employee account is not assigned to a branch");
        }

        if (requestedBranchId != null
                && !requestedBranchId.equals(employeeBranch.getId())) {
            throw new AccessDeniedException(
                    "You cannot create a customer in another branch");
        }

        return employeeBranch;
    }

    private Long getBranchScope(Employee actor) {
        if (actor.getRole() == EmployeeRole.OWNER) {
            return null; // null means organization-wide access in the query
        }

        if (actor.getBranch() == null) {
            throw new AccessDeniedException(
                    "Your employee account is not assigned to a branch");
        }

        return actor.getBranch().getId();
    }

    private void verifyAccess(Employee actor, Customer customer) {
        if (!authorizationService.canAccessCustomer(actor, customer)) {
            throw new AccessDeniedException(
                    "You cannot access customers outside your authorized branch");
        }
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found: " + id));
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BusinessRuleException(
                    "Page must be zero or greater");
        }

        if (size < 1 || size > 100) {
            throw new BusinessRuleException(
                    "Page size must be between 1 and 100");
        }
    }

    private String normalize(String value) {
        return value.trim();
    }
}
