package com.mamikos.kostapi.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, PasswordConfirmable> {

    @Override
    public boolean isValid(PasswordConfirmable request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        boolean matches = Objects.equals(request.password(), request.passwordConfirmation());
        if (!matches) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("passwordConfirmation must match password")
                    .addPropertyNode("passwordConfirmation")
                    .addConstraintViolation();
        }
        return matches;
    }
}
