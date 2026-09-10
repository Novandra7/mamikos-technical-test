package com.mamikos.kostapi.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mamikos.kostapi.common.exception.ErrorCode;
import java.util.List;
import java.util.Map;

/** Envelope shared by every failed response, including the ones Spring Security raises. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        boolean success, String message, String code, Map<String, List<String>> errors, Map<String, Object> meta) {

    public static ApiErrorResponse of(ErrorCode code, String message) {
        return of(code, message, Map.of());
    }

    public static ApiErrorResponse of(ErrorCode code, String message, Map<String, List<String>> errors) {
        return new ApiErrorResponse(false, message, code.name(), errors.isEmpty() ? null : errors, MetaFactory.base());
    }
}
