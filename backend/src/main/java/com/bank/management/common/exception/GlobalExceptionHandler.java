package com.bank.management.common.exception;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.web.server.ResponseStatusException;

import com.bank.management.branch.exception.BranchConflictException;
import com.bank.management.branch.exception.BranchNotFoundException;
import com.bank.management.common.dto.ApiErrorResponse;
import com.bank.management.common.dto.FieldValidationError;
import com.bank.management.common.enums.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                ErrorCode.ACCESS_DENIED,
                exception.getMessage(),
                List.of(),
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        List<FieldValidationError> fieldErrors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error -> new FieldValidationError(
                                error.getField(),
                                error.getDefaultMessage() == null
                                        ? "Invalid value"
                                        : error.getDefaultMessage()))
                        .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                "Request validation failed",
                fieldErrors,
                request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                "Request parameter validation failed",
                List.of(),
                request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {

        List<FieldValidationError> fieldErrors =
                exception.getConstraintViolations()
                        .stream()
                        .map(violation -> new FieldValidationError(
                                violation.getPropertyPath().toString(),
                                violation.getMessage()))
                        .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                "Request validation failed",
                fieldErrors,
                request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.MALFORMED_REQUEST,
                "Request body is malformed or contains an invalid value",
                List.of(),
                request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.MALFORMED_REQUEST,
                "Invalid value for parameter: " + exception.getName(),
                List.of(),
                request);
    }

    @ExceptionHandler(BranchNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleBranchNotFound(
            BranchNotFoundException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                ErrorCode.RESOURCE_NOT_FOUND,
                exception.getMessage(),
                List.of(),
                request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                ErrorCode.RESOURCE_NOT_FOUND,
                exception.getMessage(),
                List.of(),
                request);
    }

    @ExceptionHandler({BranchConflictException.class,
                       ResourceConflictException.class})
    public ResponseEntity<ApiErrorResponse> handleConflict(
            RuntimeException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.CONFLICT,
                ErrorCode.RESOURCE_CONFLICT,
                exception.getMessage(),
                List.of(),
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.CONFLICT,
                ErrorCode.RESOURCE_CONFLICT,
                "The operation conflicts with existing data or a database constraint",
                List.of(),
                request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(
            BusinessRuleException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ErrorCode.BUSINESS_RULE_VIOLATION,
                exception.getMessage(),
                List.of(),
                request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                ErrorCode.METHOD_NOT_ALLOWED,
                "The HTTP method is not supported for this endpoint",
                List.of(),
                request);
    }

    @ExceptionHandler(ResponseStatusException.class)
public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
        ResponseStatusException exception,
        HttpServletRequest request) {

    HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());

    ErrorCode errorCode = switch (status) {
        case UNAUTHORIZED -> ErrorCode.AUTHENTICATION_REQUIRED;
        case FORBIDDEN -> ErrorCode.ACCESS_DENIED;
        case NOT_FOUND -> ErrorCode.RESOURCE_NOT_FOUND;
        case METHOD_NOT_ALLOWED -> ErrorCode.METHOD_NOT_ALLOWED;
        case BAD_REQUEST -> ErrorCode.MALFORMED_REQUEST;
        case CONFLICT -> ErrorCode.RESOURCE_CONFLICT;
        default -> ErrorCode.INTERNAL_SERVER_ERROR;
    };

    String message = exception.getReason() != null
            ? exception.getReason()
            : status.getReasonPhrase();

    return buildResponse(
            status,
            errorCode,
            message,
            List.of(),
            request
    );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {

        log.error("Unhandled exception for {} {}",
                request.getMethod(), request.getRequestURI(), exception);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred",
                List.of(),
                request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            ErrorCode errorCode,
            String message,
            List<FieldValidationError> fieldErrors,
            HttpServletRequest request) {

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                errorCode,
                message,
                request.getRequestURI(),
                fieldErrors);

        return ResponseEntity.status(status).body(body);
    }
}