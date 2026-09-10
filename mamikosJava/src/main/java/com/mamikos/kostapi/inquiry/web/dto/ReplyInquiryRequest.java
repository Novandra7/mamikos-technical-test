package com.mamikos.kostapi.inquiry.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplyInquiryRequest(@NotBlank @Size(max = 1000) String reply) {}
