package com.mamikos.kostapi.inquiry.web.dto;

public record InquiryCreatedResponse(
        InquiryResponse inquiry, InquiryAvailabilityResponse availability, CreditChargeResponse credit) {}
