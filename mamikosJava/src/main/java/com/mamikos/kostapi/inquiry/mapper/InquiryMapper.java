package com.mamikos.kostapi.inquiry.mapper;

import com.mamikos.kostapi.inquiry.entity.AvailabilityInquiry;
import com.mamikos.kostapi.inquiry.web.dto.InquiryResponse;
import com.mamikos.kostapi.inquiry.web.dto.KostRefResponse;
import com.mamikos.kostapi.kost.entity.Kost;
import org.mapstruct.Mapper;

@Mapper
public interface InquiryMapper {

    KostRefResponse toKostRef(Kost kost);

    InquiryResponse toResponse(AvailabilityInquiry inquiry);
}
