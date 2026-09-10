package com.mamikos.kostapi.common.exception;

import org.springframework.http.HttpStatus;

/**
 * The full catalogue of machine-readable error codes the API can return.
 *
 * <p>Clients branch on {@code code}, never on the human-readable message, so the
 * message stays free to change without breaking an integration.
 */
public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_KOST_OWNER(HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT),
    INQUIRY_ALREADY_ANSWERED(HttpStatus.CONFLICT),
    DATA_CONFLICT(HttpStatus.CONFLICT),
    VALIDATION_ERROR(HttpStatus.UNPROCESSABLE_ENTITY),
    INSUFFICIENT_CREDIT(HttpStatus.UNPROCESSABLE_ENTITY),
    CREDIT_WALLET_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
