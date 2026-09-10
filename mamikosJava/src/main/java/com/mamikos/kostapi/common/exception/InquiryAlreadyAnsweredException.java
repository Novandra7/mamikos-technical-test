package com.mamikos.kostapi.common.exception;

public class InquiryAlreadyAnsweredException extends DomainException {

    public InquiryAlreadyAnsweredException() {
        super(ErrorCode.INQUIRY_ALREADY_ANSWERED, "This inquiry has already been answered");
    }
}
