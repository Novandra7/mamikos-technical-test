package com.mamikos.kostapi.common.validation;

/** Implemented by any request DTO with a total-rooms / available-rooms pair, so the
 * capacity check (BR-12: available never exceeds total) is written once. */
public interface RoomCapacityBounded {

    Integer totalRoomsValue();

    Integer availableRoomsValue();
}
