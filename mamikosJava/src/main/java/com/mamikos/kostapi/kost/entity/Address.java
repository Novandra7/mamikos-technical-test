package com.mamikos.kostapi.kost.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Address kept as one value object rather than five loose columns on the entity. */
@Embeddable
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Address {

    @Column(name = "address_street", nullable = false, length = 255)
    private String street;

    @Column(name = "address_district", nullable = false, length = 100)
    private String district;

    @Column(name = "address_city", nullable = false, length = 100)
    private String city;

    @Column(name = "address_province", nullable = false, length = 100)
    private String province;

    @Column(name = "postal_code", length = 10)
    private String postalCode;
}
