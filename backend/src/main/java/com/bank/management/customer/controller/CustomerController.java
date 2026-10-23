package com.bank.management.customer.controller;

import com.bank.management.customer.dto.CustomerCreateRequest;
import com.bank.management.customer.dto.CustomerResponse;
import com.bank.management.customer.dto.CustomerUpdateRequest;
import com.bank.management.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.createCustomer(request));
    }

    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                customerService.getCustomers(page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CustomerResponse>> searchCustomers(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                customerService.searchCustomers(query, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerUpdateRequest request
    ) {
        return ResponseEntity.ok(
                customerService.updateCustomer(id, request));
    }
}
