package com.mamikos.kostapi.common.exception;

public class UnauthenticatedException extends DomainException {

    public UnauthenticatedException(String message) {
        super(ErrorCode.UNAUTHENTICATED, message);
    }
}
