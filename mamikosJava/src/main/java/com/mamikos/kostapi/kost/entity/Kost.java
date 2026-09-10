package com.mamikos.kostapi.kost.entity;

import com.mamikos.kostapi.common.audit.BaseAuditableEntity;
import com.mamikos.kostapi.user.entity.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "kosts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Kost extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Embedded
    private Address address;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    /** Money is {@code BigDecimal}; a double would quietly lose rupiah to rounding. */
    @Column(name = "price_per_month", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    private RoomType roomType;

    @Column(name = "total_rooms", nullable = false)
    private int totalRooms;

    /**
     * Never rendered by the public search or detail endpoints. It is disclosed only in the
     * response to a paid availability inquiry, which is what the five credits actually buy.
     */
    @Column(name = "available_rooms", nullable = false)
    private int availableRooms;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "kost_facilities", joinColumns = @JoinColumn(name = "kost_id"))
    @Column(name = "name", nullable = false, length = 50)
    private Set<String> facilities = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "kost_photos", joinColumns = @JoinColumn(name = "kost_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "url", nullable = false, length = 500)
    private List<String> photos = new ArrayList<>();

    /**
     * {@code active} has no builder default (Lombok's {@code @Builder.Default} does not
     * combine reliably with a constructor-level {@code @Builder}), so every caller must
     * pass it explicitly — {@link com.mamikos.kostapi.kost.service.KostService} always
     * does.
     */
    @Builder
    @SuppressWarnings("checkstyle:ParameterNumber")
    private Kost(
            User owner,
            String name,
            String description,
            Address address,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal pricePerMonth,
            RoomType roomType,
            int totalRooms,
            int availableRooms,
            boolean active,
            Set<String> facilities,
            List<String> photos) {
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.pricePerMonth = pricePerMonth;
        this.roomType = roomType;
        this.totalRooms = totalRooms;
        this.availableRooms = availableRooms;
        this.active = active;
        replaceFacilities(facilities);
        replacePhotos(photos);
    }

    public boolean isOwnedBy(Long userId) {
        return owner != null && owner.getId() != null && owner.getId().equals(userId);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isPubliclyVisible() {
        return active && !isDeleted();
    }

    public void softDelete(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void replaceFacilities(Set<String> newFacilities) {
        this.facilities.clear();
        if (newFacilities != null) {
            this.facilities.addAll(newFacilities);
        }
    }

    public void replacePhotos(List<String> newPhotos) {
        this.photos.clear();
        if (newPhotos != null) {
            this.photos.addAll(newPhotos);
        }
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public void updateDetails(
            String name,
            String description,
            Address address,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal pricePerMonth,
            RoomType roomType,
            int totalRooms,
            int availableRooms,
            boolean active) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.pricePerMonth = pricePerMonth;
        this.roomType = roomType;
        this.totalRooms = totalRooms;
        this.availableRooms = availableRooms;
        this.active = active;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void changeAddress(Address address) {
        this.address = address;
    }

    public void changeCoordinates(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void changePrice(BigDecimal pricePerMonth) {
        this.pricePerMonth = pricePerMonth;
    }

    public void changeRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public void changeCapacity(int totalRooms, int availableRooms) {
        this.totalRooms = totalRooms;
        this.availableRooms = availableRooms;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }
}
