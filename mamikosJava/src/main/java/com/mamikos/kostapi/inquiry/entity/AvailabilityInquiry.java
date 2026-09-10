package com.mamikos.kostapi.inquiry.entity;

import com.mamikos.kostapi.common.audit.BaseAuditableEntity;
import com.mamikos.kostapi.common.exception.InquiryAlreadyAnsweredException;
import com.mamikos.kostapi.kost.entity.Kost;
import com.mamikos.kostapi.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "availability_inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AvailabilityInquiry extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kost_id", nullable = false)
    private Kost kost;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "text")
    private String message;

    @Column(name = "credit_charged", nullable = false)
    private int creditCharged;

    /**
     * What availability looked like when the question was asked. Storing the snapshot keeps
     * the record honest: the owner may change the room count minutes later, and the user
     * paid for the number they were shown.
     */
    @Column(name = "available_rooms_snapshot", nullable = false)
    private int availableRoomsSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status = InquiryStatus.PENDING;

    @Column(name = "owner_reply", columnDefinition = "text")
    private String ownerReply;

    @Column(name = "replied_at")
    private Instant repliedAt;

    private AvailabilityInquiry(Kost kost, User user, String message, int creditCharged) {
        this.kost = kost;
        this.user = user;
        this.message = message;
        this.creditCharged = creditCharged;
        this.availableRoomsSnapshot = kost.getAvailableRooms();
        this.status = InquiryStatus.PENDING;
    }

    public static AvailabilityInquiry raise(Kost kost, User user, String message, int creditCharged) {
        return new AvailabilityInquiry(kost, user, message, creditCharged);
    }

    public void reply(String reply, Instant repliedAt) {
        if (status == InquiryStatus.ANSWERED) {
            throw new InquiryAlreadyAnsweredException();
        }
        this.ownerReply = reply;
        this.repliedAt = repliedAt;
        this.status = InquiryStatus.ANSWERED;
    }

    public boolean hasRoomAvailable() {
        return availableRoomsSnapshot > 0;
    }
}
