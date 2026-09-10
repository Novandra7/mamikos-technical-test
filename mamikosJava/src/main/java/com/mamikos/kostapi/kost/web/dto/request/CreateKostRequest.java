package com.mamikos.kostapi.kost.web.dto.request;

import com.mamikos.kostapi.common.validation.AvailableRoomsWithinCapacity;
import com.mamikos.kostapi.common.validation.RoomCapacityBounded;
import com.mamikos.kostapi.kost.entity.RoomType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.hibernate.validator.constraints.URL;

@AvailableRoomsWithinCapacity
public record CreateKostRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 4000) String description,
        @NotNull @Valid AddressRequest address,
        BigDecimal latitude,
        BigDecimal longitude,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal pricePerMonth,
        @NotNull RoomType roomType,
        @Positive int totalRooms,
        @PositiveOrZero int availableRooms,
        Boolean isActive,
        @Size(max = 20) Set<@NotBlank @Size(max = 50) String> facilities,
        @Size(max = 10) List<@NotBlank @URL @Size(max = 500) String> photos)
        implements RoomCapacityBounded {

    @Override
    public Integer totalRoomsValue() {
        return totalRooms;
    }

    @Override
    public Integer availableRoomsValue() {
        return availableRooms;
    }
}
