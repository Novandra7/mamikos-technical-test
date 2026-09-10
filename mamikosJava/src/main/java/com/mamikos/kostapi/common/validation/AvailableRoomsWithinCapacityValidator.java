package com.mamikos.kostapi.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AvailableRoomsWithinCapacityValidator
        implements ConstraintValidator<AvailableRoomsWithinCapacity, RoomCapacityBounded> {

    @Override
    public boolean isValid(RoomCapacityBounded request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        Integer total = request.totalRoomsValue();
        Integer available = request.availableRoomsValue();
        // Only compare when both are present; a PATCH may legitimately supply just one,
        // and the service layer checks that case against the persisted value instead.
        if (total == null || available == null) {
            return true;
        }
        boolean valid = available <= total;
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("availableRooms must not exceed totalRooms")
                    .addPropertyNode("availableRooms")
                    .addConstraintViolation();
        }
        return valid;
    }
}
