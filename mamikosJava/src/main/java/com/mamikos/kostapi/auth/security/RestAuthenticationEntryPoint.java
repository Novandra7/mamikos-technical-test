package com.mamikos.kostapi.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mamikos.kostapi.common.exception.ErrorCode;
import com.mamikos.kostapi.common.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Without this, Spring Security's default entry point sends its own error body on a 401,
 * a shape that matches none of the other endpoints. Every response from this API — success
 * or failure — goes through the same envelope, and this is what keeps 401 consistent with it.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(ErrorCode.UNAUTHENTICATED.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiErrorResponse.of(ErrorCode.UNAUTHENTICATED, "Authentication is required to access this resource"));
    }
}
