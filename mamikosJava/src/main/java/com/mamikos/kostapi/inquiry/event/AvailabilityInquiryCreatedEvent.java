package com.mamikos.kostapi.inquiry.event;

public record AvailabilityInquiryCreatedEvent(Long inquiryId, Long kostId, Long ownerId, Long userId) {}
