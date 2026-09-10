package com.mamikos.kostapi.kost.mapper;

import com.mamikos.kostapi.kost.entity.Address;
import com.mamikos.kostapi.kost.entity.Kost;
import com.mamikos.kostapi.kost.web.dto.response.AddressResponse;
import com.mamikos.kostapi.kost.web.dto.response.AvailabilityResponse;
import com.mamikos.kostapi.kost.web.dto.response.KostDetailResponse;
import com.mamikos.kostapi.kost.web.dto.response.KostSummaryResponse;
import com.mamikos.kostapi.kost.web.dto.response.OwnerKostResponse;
import com.mamikos.kostapi.kost.web.dto.response.OwnerSummaryResponse;
import com.mamikos.kostapi.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface KostMapper {

    AddressResponse toAddressResponse(Address address);

    OwnerSummaryResponse toOwnerSummary(User owner);

    /**
     * Public search row. {@code availability} is always the undisclosed placeholder here,
     * never derived from the entity's real {@code availableRooms} — that value never
     * reaches a public response at all.
     */
    @Mapping(
            target = "availability",
            expression = "java(com.mamikos.kostapi.kost.web.dto.response." + "AvailabilityResponse.undisclosed())")
    KostSummaryResponse toSummaryResponse(Kost kost);

    @Mapping(
            target = "availability",
            expression = "java(com.mamikos.kostapi.kost.web.dto.response." + "AvailabilityResponse.undisclosed())")
    KostDetailResponse toDetailResponse(Kost kost);

    @Mapping(target = "isActive", source = "kost.active")
    @Mapping(target = "inquiriesCount", source = "inquiriesCount")
    OwnerKostResponse toOwnerResponse(Kost kost, long inquiriesCount);

    default AvailabilityResponse disclosedAvailability(Kost kost) {
        return AvailabilityResponse.disclosed(kost.getAvailableRooms(), kost.getTotalRooms());
    }
}
