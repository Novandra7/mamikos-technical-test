package com.mamikos.kostapi.common.exception;

import java.util.List;
import java.util.Map;

/** Base class for every business-rule violation the API reports deliberately. */
public class DomainException extends RuntimeException {

    private final transient ErrorCode errorCode;
    private final transient Map<String, List<String>> errors;

    protected DomainException(ErrorCode errorCode, String message) {
        this(errorCode, message, Map.of());
    }

    protected DomainException(ErrorCode errorCode, String message, Map<String, List<String>> errors) {
        super(message);
        this.errorCode = errorCode;
        this.errors = errors == null ? Map.of() : Map.copyOf(errors);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Map<String, List<String>> errors() {
        return errors;
    }
}
