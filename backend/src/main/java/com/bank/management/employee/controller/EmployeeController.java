package com.bank.management.employee.controller;

import com.bank.management.employee.dto.CreateEmployeeRequest;
import com.bank.management.employee.dto.EmployeeAssignmentRequest;
import com.bank.management.employee.dto.EmployeeResponse;
import com.bank.management.employee.dto.EmployeeStatusRequest;
import com.bank.management.employee.dto.UpdateEmployeeRequest;
import com.bank.management.employee.service.EmployeeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@Validated
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping("/managers")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<EmployeeResponse> createManager(
            @Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.createManager(request));
    }

    @PostMapping("/staff")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<EmployeeResponse> createStaff(
            @Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.createStaff(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    public ResponseEntity<List<EmployeeResponse>> getEmployees() {
        return ResponseEntity.ok(employeeService.getEmployees());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    public ResponseEntity<EmployeeResponse> getEmployee(
            @PathVariable @Positive Long id) {
        return ResponseEntity.ok(employeeService.getEmployee(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(
                employeeService.updateEmployee(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<EmployeeResponse> updateStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody EmployeeStatusRequest request) {
        return ResponseEntity.ok(
                employeeService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/assignment")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<EmployeeResponse> updateAssignment(
            @PathVariable @Positive Long id,
            @Valid @RequestBody EmployeeAssignmentRequest request) {
        return ResponseEntity.ok(
                employeeService.updateAssignment(id, request));
    }
}