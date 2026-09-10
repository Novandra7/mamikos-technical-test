package com.mamikos.kostapi.common.exception;

import java.util.List;
import java.util.Map;

public class EmailAlreadyRegisteredException extends DomainException {

    public EmailAlreadyRegisteredException() {
        super(
                ErrorCode.EMAIL_ALREADY_REGISTERED,
                "That email address is already registered",
                Map.of("email", List.of("This email address is already registered")));
    }
}
