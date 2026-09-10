package com.mamikos.kostapi.common.exception;

public class CreditWalletNotFoundException extends DomainException {

    public CreditWalletNotFoundException() {
        super(ErrorCode.CREDIT_WALLET_NOT_FOUND, "This account does not have a credit wallet");
    }
}
