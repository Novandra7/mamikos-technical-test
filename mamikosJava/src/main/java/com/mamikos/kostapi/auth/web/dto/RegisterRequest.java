package com.mamikos.kostapi.auth.web.dto;

import com.mamikos.kostapi.common.validation.PasswordConfirmable;
import com.mamikos.kostapi.common.validation.PasswordMatches;
import com.mamikos.kostapi.user.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@PasswordMatches
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank
                @Size(min = 8, max = 100)
                @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "password must contain a letter and a digit")
                String password,
        @NotBlank String passwordConfirmation,
        @Pattern(regexp = "^08\\d{8,11}$", message = "phone must be a valid Indonesian mobile number") String phone,
        @NotNull UserRole role)
        implements PasswordConfirmable {}
