package com.mamikos.kostapi.kost.web.dto.response;

/**
 * What five credits buy: {@code disclosed = false} everywhere public (search, detail);
 * {@code disclosed = true} with the real numbers only in the paid inquiry response.
 */
public record AvailabilityResponse(boolean disclosed, String hint, Integer availableRooms, Integer totalRooms) {

    public static AvailabilityResponse undisclosed() {
        return new AvailabilityResponse(false, "Ask the owner to reveal room availability", null, null);
    }

    public static AvailabilityResponse disclosed(int availableRooms, int totalRooms) {
        return new AvailabilityResponse(true, null, availableRooms, totalRooms);
    }
}
