package com.bank.management.common.exception;

import com.bank.management.branch.exception.BranchConflictException;
import com.bank.management.branch.exception.BranchNotFoundException;
import com.bank.management.common.dto.ApiErrorResponse;
import com.bank.management.common.enums.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/branches");
    }

    @Test
    void branchNotFoundReturnsStandard404Response() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleBranchNotFound(
                        new BranchNotFoundException(99L),
                        request
                );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND,
                response.getBody().errorCode());
        assertEquals("/api/branches", response.getBody().path());
        assertNotNull(response.getBody().timestamp());
        assertTrue(response.getBody().message().contains("99"));
        assertTrue(response.getBody().fieldErrors().isEmpty());
    }

    @Test
    void branchConflictReturnsStandard409Response() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleConflict(
                        new BranchConflictException("Branch ID already exists"),
                        request
                );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.RESOURCE_CONFLICT,
                response.getBody().errorCode());
        assertEquals("Branch ID already exists",
                response.getBody().message());
    }

    @Test
    void businessRuleReturnsStandard422Response() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleBusinessRule(
                        new BusinessRuleException(
                                "Account balance must be zero"
                        ),
                        request
                );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY,
                response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.BUSINESS_RULE_VIOLATION,
                response.getBody().errorCode());
        assertEquals("Account balance must be zero",
                response.getBody().message());
    }

    @Test
    void validationReturnsFieldErrors() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(
                        new Object(),
                        "branchRequest"
                );

        bindingResult.addError(new FieldError(
                "branchRequest",
                "branchName",
                "Branch name is required"
        ));

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiErrorResponse> response =
                handler.handleValidation(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.VALIDATION_ERROR,
                response.getBody().errorCode());
        assertEquals(1, response.getBody().fieldErrors().size());
        assertEquals("branchName",
                response.getBody().fieldErrors().get(0).field());
        assertEquals("Branch name is required",
                response.getBody().fieldErrors().get(0).message());
    }
}