package com.mamikos.kostapi.common.exception;

import java.util.List;
import java.util.Map;

public class InsufficientCreditException extends DomainException {

    private final transient int required;
    private final transient int currentBalance;

    public InsufficientCreditException(int required, int currentBalance) {
        super(
                ErrorCode.INSUFFICIENT_CREDIT,
                "Your credit balance is not enough to ask about room availability",
                Map.of(
                        "credit",
                        List.of("Required %d credit, current balance %d".formatted(required, currentBalance))));
        this.required = required;
        this.currentBalance = currentBalance;
    }

    public int required() {
        return required;
    }

    public int currentBalance() {
        return currentBalance;
    }
}
