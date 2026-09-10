package com.mamikos.kostapi.common.validation;

/**
 * Implemented by any request DTO that carries a password and its confirmation, so
 * {@link PasswordMatchesValidator} can validate them without depending on a specific
 * feature package. Keeps {@code common} free of a reverse dependency on {@code auth}.
 */
public interface PasswordConfirmable {

    String password();

    String passwordConfirmation();
}
