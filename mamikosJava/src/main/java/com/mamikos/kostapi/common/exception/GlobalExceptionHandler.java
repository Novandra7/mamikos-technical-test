package com.mamikos.kostapi.common.exception;

import com.mamikos.kostapi.common.response.ApiErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * The single place every exception this API can raise turns into the standard
 * {@link ApiErrorResponse} envelope.
 *
 * <p>{@code RestAccessDeniedHandler} and {@code RestAuthenticationEntryPoint} cover denials
 * that the security *filter chain* itself raises (a request that never reaches a
 * controller at all). But {@code @PreAuthorize} is method security: it throws
 * {@code AccessDeniedException} — as of Spring Security 6.3+, specifically
 * {@code AuthorizationDeniedException} — from inside the controller method invocation,
 * which Spring MVC resolves through this advice before it ever reaches the filter chain.
 * Without an explicit branch for it here, it would fall through to the generic handler
 * below and come back as a 500 instead of a 403 — which is exactly the bug this class once
 * had. {@link BadCredentialsException} has the same shape of problem: it is thrown from
 * application code in {@code AuthService}, not by the filter chain, so it needs its own
 * branch to get the more specific {@code INVALID_CREDENTIALS} code.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.status())
                .body(ApiErrorResponse.of(ErrorCode.FORBIDDEN, "You do not have permission to perform this action"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(ErrorCode.UNAUTHENTICATED.status())
                .body(ApiErrorResponse.of(
                        ErrorCode.UNAUTHENTICATED, "Authentication is required to access this resource"));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainException(DomainException ex) {
        return ResponseEntity.status(ex.errorCode().status())
                .body(ApiErrorResponse.of(ex.errorCode(), ex.getMessage(), ex.errors()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(ErrorCode.INVALID_CREDENTIALS.status())
                .body(ApiErrorResponse.of(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(fieldError.getField(), key -> new ArrayList<>())
                    .add(fieldError.getDefaultMessage());
        }
        for (var globalError : ex.getBindingResult().getGlobalErrors()) {
            errors.computeIfAbsent("_", key -> new ArrayList<>()).add(globalError.getDefaultMessage());
        }
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.status())
                .body(ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR, "Validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            errors.computeIfAbsent(field, key -> new ArrayList<>()).add(violation.getMessage());
        }
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.status())
                .body(ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR, "Validation failed", errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(ErrorCode.BAD_REQUEST.status())
                .body(ApiErrorResponse.of(ErrorCode.BAD_REQUEST, "'%s' has an invalid value".formatted(ex.getName())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(ErrorCode.BAD_REQUEST.status())
                .body(ApiErrorResponse.of(ErrorCode.BAD_REQUEST, "Request body is missing or malformed JSON"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.status())
                .body(ApiErrorResponse.of(
                        ErrorCode.METHOD_NOT_ALLOWED,
                        "HTTP method '%s' is not supported here".formatted(ex.getMethod())));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNoHandler() {
        return ResponseEntity.status(ErrorCode.RESOURCE_NOT_FOUND.status())
                .body(ApiErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND, "The requested endpoint does not exist"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        // A constraint violation that reaches here (rather than a specific DomainException
        // raised ahead of the write) is unexpected — log the real cause, but never let a
        // raw SQL or constraint name reach the client.
        log.warn("Unhandled data integrity violation", ex);
        return ResponseEntity.status(ErrorCode.DATA_CONFLICT.status())
                .body(ApiErrorResponse.of(ErrorCode.DATA_CONFLICT, "The request conflicts with existing data"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.status())
                .body(ApiErrorResponse.of(
                        ErrorCode.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later."));
    }
}
