package com.mamikos.kostapi.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** Envelope shared by every successful response so clients parse one shape. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data, Map<String, Object> meta) {

    public static <T> ApiResponse<T> of(String message, T data) {
        return new ApiResponse<>(true, message, data, MetaFactory.base());
    }

    public static <T> ApiResponse<T> of(String message, T data, Map<String, Object> extraMeta) {
        Map<String, Object> meta = MetaFactory.base();
        meta.putAll(extraMeta);
        return new ApiResponse<>(true, message, data, meta);
    }
}
