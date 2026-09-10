package com.mamikos.kostapi.inquiry.web.dto;

import jakarta.validation.constraints.Size;

public record CreateInquiryRequest(@Size(max = 1000) String message) {}
