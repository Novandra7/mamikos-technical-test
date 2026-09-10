package com.mamikos.kostapi.common.exception;

import java.util.List;
import java.util.Map;

/** For validation failures raised by hand-written service logic, e.g. a whitelist check
 * that Bean Validation annotations cannot express. Bean Validation itself is handled
 * separately, by the {@code MethodArgumentNotValidException} branch in the global handler. */
public class ValidationException extends DomainException {

    public ValidationException(String field, String message) {
        super(ErrorCode.VALIDATION_ERROR, "Validation failed", Map.of(field, List.of(message)));
    }

    public ValidationException(Map<String, List<String>> errors) {
        super(ErrorCode.VALIDATION_ERROR, "Validation failed", errors);
    }
}
