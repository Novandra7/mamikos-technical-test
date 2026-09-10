package com.mamikos.kostapi.common.exception;

public class NotKostOwnerException extends DomainException {

    public NotKostOwnerException() {
        super(ErrorCode.NOT_KOST_OWNER, "You can only manage kosts that belong to you");
    }
}
