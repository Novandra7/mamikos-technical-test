package com.mamikos.kostapi.inquiry.service;

import com.mamikos.kostapi.common.exception.NotKostOwnerException;
import com.mamikos.kostapi.common.exception.ResourceNotFoundException;
import com.mamikos.kostapi.credit.config.CreditProperties;
import com.mamikos.kostapi.credit.entity.CreditBalance;
import com.mamikos.kostapi.credit.service.CreditService;
import com.mamikos.kostapi.inquiry.entity.AvailabilityInquiry;
import com.mamikos.kostapi.inquiry.event.AvailabilityInquiryCreatedEvent;
import com.mamikos.kostapi.inquiry.mapper.InquiryMapper;
import com.mamikos.kostapi.inquiry.repository.AvailabilityInquiryRepository;
import com.mamikos.kostapi.inquiry.web.dto.CreateInquiryRequest;
import com.mamikos.kostapi.inquiry.web.dto.CreditChargeResponse;
import com.mamikos.kostapi.inquiry.web.dto.InquiryAvailabilityResponse;
import com.mamikos.kostapi.inquiry.web.dto.InquiryCreatedResponse;
import com.mamikos.kostapi.inquiry.web.dto.InquiryResponse;
import com.mamikos.kostapi.inquiry.web.dto.ReplyInquiryRequest;
import com.mamikos.kostapi.kost.entity.Kost;
import com.mamikos.kostapi.kost.repository.KostRepository;
import com.mamikos.kostapi.user.entity.User;
import com.mamikos.kostapi.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Charges five credits (BR-02) to reveal a kost's real room availability and forward the
 * question to its owner (US-08).
 *
 * <p>{@link #create} is the one place BR-03 actually gets enforced end to end: the wallet
 * row is locked, the deduction is validated against it, and only once that succeeds does
 * the inquiry get written — so a rejected inquiry (insufficient credit, wrong role, kost
 * gone) leaves absolutely no trace and the balance untouched, exactly as the spec requires.
 */
@Service
@Transactional
public class AvailabilityInquiryService {

    private final KostRepository kostRepository;
    private final UserRepository userRepository;
    private final AvailabilityInquiryRepository inquiryRepository;
    private final CreditService creditService;
    private final CreditProperties creditProperties;
    private final InquiryMapper inquiryMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public AvailabilityInquiryService(
            KostRepository kostRepository,
            UserRepository userRepository,
            AvailabilityInquiryRepository inquiryRepository,
            CreditService creditService,
            CreditProperties creditProperties,
            InquiryMapper inquiryMapper,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.kostRepository = kostRepository;
        this.userRepository = userRepository;
        this.inquiryRepository = inquiryRepository;
        this.creditService = creditService;
        this.creditProperties = creditProperties;
        this.inquiryMapper = inquiryMapper;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public InquiryCreatedResponse create(Long userId, Long kostId, CreateInquiryRequest request) {
        Kost kost =
                kostRepository.findActiveById(kostId).orElseThrow(() -> new ResourceNotFoundException("Kost", kostId));
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));

        int cost = creditProperties.inquiryCost();

        // Lock first, deduct before anything else is written: if the balance cannot cover
        // the cost, CreditBalance#deduct throws here and nothing about this inquiry ever
        // touches the database — no half-written row for the transaction to roll back.
        CreditBalance balance = creditService.lockBalance(userId);
        int before = balance.getBalance();
        balance.deduct(cost);

        AvailabilityInquiry inquiry =
                inquiryRepository.save(AvailabilityInquiry.raise(kost, user, request.message(), cost));
        creditService.recordDeduction(user, cost, before, inquiry.getId());

        eventPublisher.publishEvent(new AvailabilityInquiryCreatedEvent(
                inquiry.getId(), kost.getId(), kost.getOwner().getId(), userId));

        InquiryResponse inquiryResponse = inquiryMapper.toResponse(inquiry);
        InquiryAvailabilityResponse availability = new InquiryAvailabilityResponse(
                true,
                inquiry.getAvailableRoomsSnapshot(),
                kost.getTotalRooms(),
                inquiry.hasRoomAvailable(),
                Instant.now(clock));
        CreditChargeResponse credit = new CreditChargeResponse(cost, before, balance.getBalance());

        return new InquiryCreatedResponse(inquiryResponse, availability, credit);
    }

    @Transactional(readOnly = true)
    public Page<InquiryResponse> myInquiries(Long userId, Pageable pageable) {
        return inquiryRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(inquiryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<InquiryResponse> forOwner(Long ownerId, String statusFilter, Pageable pageable) {
        Page<AvailabilityInquiry> page = statusFilter == null
                ? inquiryRepository.findByOwnerId(ownerId, pageable)
                : inquiryRepository.findByOwnerIdAndStatus(
                        ownerId, com.mamikos.kostapi.inquiry.entity.InquiryStatus.valueOf(statusFilter), pageable);
        return page.map(inquiryMapper::toResponse);
    }

    public InquiryResponse reply(Long ownerId, Long inquiryId, ReplyInquiryRequest request) {
        AvailabilityInquiry inquiry = inquiryRepository
                .findByIdAndOwnerId(inquiryId, ownerId)
                .orElseThrow(() -> {
                    // Distinguishing "doesn't exist" from "exists but isn't yours" would leak
                    // which inquiry ids are in use; both cases return the same 403/404 pairing
                    // used elsewhere for owned resources (see NotKostOwnerException).
                    boolean exists = inquiryRepository.existsById(inquiryId);
                    return exists ? new NotKostOwnerException() : new ResourceNotFoundException("Inquiry", inquiryId);
                });
        inquiry.reply(request.reply(), Instant.now(clock));
        return inquiryMapper.toResponse(inquiry);
    }
}
