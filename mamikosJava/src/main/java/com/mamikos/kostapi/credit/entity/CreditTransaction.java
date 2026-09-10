package com.mamikos.kostapi.credit.entity;

import com.mamikos.kostapi.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * One immutable line in the credit ledger.
 *
 * <p>The table is append-only: nothing ever updates or deletes a row. A mistake is fixed
 * by writing a compensating {@code ADJUSTMENT} entry, which keeps the history of what the
 * balance actually did intact and auditable.
 */
@Entity
@Table(name = "credit_transactions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CreditTransactionType type;

    /** Signed: positive for a grant or recharge, negative for a deduction. */
    @Column(nullable = false)
    private int amount;

    @Column(name = "balance_before", nullable = false)
    private int balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 255)
    private String description;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @SuppressWarnings("checkstyle:ParameterNumber")
    private CreditTransaction(
            User user,
            CreditTransactionType type,
            int amount,
            int balanceBefore,
            int balanceAfter,
            String referenceType,
            Long referenceId,
            String description) {
        this.user = user;
        this.type = type;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.description = description;
    }

    public static CreditTransaction initialGrant(User user, int amount) {
        return new CreditTransaction(
                user,
                CreditTransactionType.INITIAL_GRANT,
                amount,
                0,
                amount,
                null,
                null,
                "Initial credit grant for %s account"
                        .formatted(user.getRole().name().toLowerCase()));
    }

    public static CreditTransaction monthlyRecharge(User user, int balanceBefore, int balanceAfter, String period) {
        return new CreditTransaction(
                user,
                CreditTransactionType.MONTHLY_RECHARGE,
                balanceAfter - balanceBefore,
                balanceBefore,
                balanceAfter,
                null,
                null,
                "Monthly credit recharge for %s".formatted(period));
    }

    public static CreditTransaction inquiryDeduction(User user, int cost, int balanceBefore, Long inquiryId) {
        return new CreditTransaction(
                user,
                CreditTransactionType.INQUIRY_DEDUCTION,
                -cost,
                balanceBefore,
                balanceBefore - cost,
                "AVAILABILITY_INQUIRY",
                inquiryId,
                "Room availability inquiry");
    }
}
