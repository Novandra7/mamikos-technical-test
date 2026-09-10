package com.mamikos.kostapi.auth.web;

import com.mamikos.kostapi.auth.security.BearerTokenExtractor;
import com.mamikos.kostapi.auth.security.JwtService;
import com.mamikos.kostapi.auth.security.SecurityUser;
import com.mamikos.kostapi.auth.service.AuthService;
import com.mamikos.kostapi.auth.web.dto.AuthResponse;
import com.mamikos.kostapi.auth.web.dto.LoginRequest;
import com.mamikos.kostapi.auth.web.dto.MeResponse;
import com.mamikos.kostapi.auth.web.dto.RefreshRequest;
import com.mamikos.kostapi.auth.web.dto.RegisterRequest;
import com.mamikos.kostapi.auth.web.dto.TokenResponse;
import com.mamikos.kostapi.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, refresh, logout, and the current profile")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.of("Registration successful", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.of("Login successful", authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.of("Token refreshed", authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal SecurityUser principal, HttpServletRequest request) {
        var currentToken = BearerTokenExtractor.extract(request)
                .flatMap(jwtService::parseAccessTokenClaims)
                .orElse(null);
        authService.logout(principal.id(), currentToken);
        return ApiResponse.of("Logged out successfully", null);
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal SecurityUser principal) {
        return ApiResponse.of("Profile retrieved", authService.me(principal.id()));
    }
}
