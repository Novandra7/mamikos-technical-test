package com.mamikos.kostapi.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = AvailableRoomsWithinCapacityValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AvailableRoomsWithinCapacity {

    String message() default "availableRooms must not exceed totalRooms";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
