package com.mamikos.kostapi.kost.service;

import com.mamikos.kostapi.common.exception.NotKostOwnerException;
import com.mamikos.kostapi.common.exception.ResourceNotFoundException;
import com.mamikos.kostapi.common.exception.ValidationException;
import com.mamikos.kostapi.inquiry.repository.AvailabilityInquiryRepository;
import com.mamikos.kostapi.kost.entity.Address;
import com.mamikos.kostapi.kost.entity.Kost;
import com.mamikos.kostapi.kost.mapper.KostMapper;
import com.mamikos.kostapi.kost.repository.KostRepository;
import com.mamikos.kostapi.kost.web.dto.request.AddressRequest;
import com.mamikos.kostapi.kost.web.dto.request.CreateKostRequest;
import com.mamikos.kostapi.kost.web.dto.request.PatchKostRequest;
import com.mamikos.kostapi.kost.web.dto.request.UpdateKostRequest;
import com.mamikos.kostapi.kost.web.dto.response.KostDetailResponse;
import com.mamikos.kostapi.kost.web.dto.response.OwnerKostResponse;
import com.mamikos.kostapi.user.entity.User;
import com.mamikos.kostapi.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owner-facing kost CRUD.
 *
 * <p>Every mutating method re-derives the kost's owner from the persisted entity and
 * compares it to the caller's id — never the other way around — so a client cannot use a
 * crafted request body to act on someone else's listing (BR-06).
 */
@Service
@Transactional
public class KostService {

    private final KostRepository kostRepository;
    private final UserRepository userRepository;
    private final AvailabilityInquiryRepository inquiryRepository;
    private final KostMapper kostMapper;
    private final Clock clock;

    public KostService(
            KostRepository kostRepository,
            UserRepository userRepository,
            AvailabilityInquiryRepository inquiryRepository,
            KostMapper kostMapper,
            Clock clock) {
        this.kostRepository = kostRepository;
        this.userRepository = userRepository;
        this.inquiryRepository = inquiryRepository;
        this.kostMapper = kostMapper;
        this.clock = clock;
    }

    public OwnerKostResponse create(Long ownerId, CreateKostRequest request) {
        User owner = userRepository.findById(ownerId).orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

        Kost kost = Kost.builder()
                .owner(owner)
                .name(request.name())
                .description(request.description())
                .address(toAddress(request.address()))
                .latitude(request.latitude())
                .longitude(request.longitude())
                .pricePerMonth(request.pricePerMonth())
                .roomType(request.roomType())
                .totalRooms(request.totalRooms())
                .availableRooms(request.availableRooms())
                .active(request.isActive() == null || request.isActive())
                .facilities(request.facilities())
                .photos(request.photos())
                .build();

        Kost saved = kostRepository.save(kost);
        return kostMapper.toOwnerResponse(saved, 0L);
    }

    @Transactional(readOnly = true)
    public Page<OwnerKostResponse> listForOwner(Long ownerId, boolean includeDeleted, Pageable pageable) {
        Page<Kost> page = includeDeleted
                ? kostRepository.findAllByOwnerIdIncludingDeleted(ownerId, pageable)
                : kostRepository.findAllByOwnerId(ownerId, pageable);
        return page.map(kost -> kostMapper.toOwnerResponse(kost, inquiryRepository.countByKostId(kost.getId())));
    }

    @Transactional(readOnly = true)
    public OwnerKostResponse getOwned(Long ownerId, Long kostId) {
        Kost kost = requireOwned(ownerId, kostId);
        return kostMapper.toOwnerResponse(kost, inquiryRepository.countByKostId(kostId));
    }

    public OwnerKostResponse replace(Long ownerId, Long kostId, UpdateKostRequest request) {
        Kost kost = requireOwned(ownerId, kostId);
        kost.updateDetails(
                request.name(),
                request.description(),
                toAddress(request.address()),
                request.latitude(),
                request.longitude(),
                request.pricePerMonth(),
                request.roomType(),
                request.totalRooms(),
                request.availableRooms(),
                request.isActive());
        kost.replaceFacilities(request.facilities());
        kost.replacePhotos(request.photos());
        return kostMapper.toOwnerResponse(kost, inquiryRepository.countByKostId(kostId));
    }

    public OwnerKostResponse patch(Long ownerId, Long kostId, PatchKostRequest request) {
        Kost kost = requireOwned(ownerId, kostId);

        if (request.name() != null) {
            kost.changeName(request.name());
        }
        if (request.description() != null) {
            kost.changeDescription(request.description());
        }
        if (request.address() != null) {
            kost.changeAddress(toAddress(request.address()));
        }
        if (request.latitude() != null || request.longitude() != null) {
            kost.changeCoordinates(
                    request.latitude() != null ? request.latitude() : kost.getLatitude(),
                    request.longitude() != null ? request.longitude() : kost.getLongitude());
        }
        if (request.pricePerMonth() != null) {
            kost.changePrice(request.pricePerMonth());
        }
        if (request.roomType() != null) {
            kost.changeRoomType(request.roomType());
        }
        applyCapacityPatch(kost, request);
        if (request.isActive() != null) {
            kost.changeActive(request.isActive());
        }
        if (request.facilities() != null) {
            kost.replaceFacilities(request.facilities());
        }
        if (request.photos() != null) {
            kost.replacePhotos(request.photos());
        }

        return kostMapper.toOwnerResponse(kost, inquiryRepository.countByKostId(kostId));
    }

    public void softDelete(Long ownerId, Long kostId) {
        Kost kost = requireOwned(ownerId, kostId);
        kost.softDelete(Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public KostDetailResponse getPublicDetail(Long kostId) {
        Kost kost =
                kostRepository.findActiveById(kostId).orElseThrow(() -> new ResourceNotFoundException("Kost", kostId));
        return kostMapper.toDetailResponse(kost);
    }

    private Kost requireOwned(Long ownerId, Long kostId) {
        Kost kost = kostRepository
                .findActiveOrInactiveById(kostId)
                .orElseThrow(() -> new ResourceNotFoundException("Kost", kostId));
        if (!kost.isOwnedBy(ownerId)) {
            throw new NotKostOwnerException();
        }
        return kost;
    }

    /**
     * A PATCH may touch only one of the two capacity fields; the other side of the
     * comparison then has to be the value already on the entity, which is exactly what
     * the DTO-level {@code @AvailableRoomsWithinCapacity} constraint cannot see.
     */
    private void applyCapacityPatch(Kost kost, PatchKostRequest request) {
        if (request.totalRooms() == null && request.availableRooms() == null) {
            return;
        }
        int newTotal = request.totalRooms() != null ? request.totalRooms() : kost.getTotalRooms();
        int newAvailable = request.availableRooms() != null ? request.availableRooms() : kost.getAvailableRooms();
        if (newAvailable > newTotal) {
            throw new ValidationException("availableRooms", "availableRooms must not exceed totalRooms");
        }
        kost.changeCapacity(newTotal, newAvailable);
    }

    private Address toAddress(AddressRequest request) {
        return Address.builder()
                .street(request.street())
                .district(request.district())
                .city(request.city())
                .province(request.province())
                .postalCode(request.postalCode())
                .build();
    }
}
