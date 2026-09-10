package com.mamikos.kostapi.credit.entity;

import com.mamikos.kostapi.common.audit.BaseAuditableEntity;
import com.mamikos.kostapi.common.exception.InsufficientCreditException;
import com.mamikos.kostapi.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A user's credit wallet.
 *
 * <p>Owners have no row here at all. Modelling "no credit" as a missing wallet rather than
 * a zero balance means the rule is enforced by the data model, not only by a check in a
 * service that someone could forget to call.
 */
@Entity
@Table(name = "credit_balances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditBalance extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private int balance;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "last_recharged_at")
    private Instant lastRechargedAt;

    private CreditBalance(User user, int initialBalance) {
        this.user = user;
        this.balance = initialBalance;
    }

    public static CreditBalance openFor(User user, int initialBalance) {
        return new CreditBalance(user, initialBalance);
    }

    /**
     * Subtracts credit, refusing to go negative.
     *
     * <p>This guard is the innermost of three: the row is locked before the read, the
     * database carries a {@code CHECK (balance >= 0)} constraint, and this method rejects
     * the operation outright. Any one of them alone would eventually let a concurrent pair
     * of requests overdraw the wallet.
     */
    public void deduct(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Deduction amount must not be negative");
        }
        if (balance < amount) {
            throw new InsufficientCreditException(amount, balance);
        }
        this.balance -= amount;
    }

    public void applyRecharge(int newBalance, Instant rechargedAt) {
        if (newBalance < 0) {
            throw new IllegalArgumentException("Recharged balance must not be negative");
        }
        this.balance = newBalance;
        this.lastRechargedAt = rechargedAt;
    }

    public boolean canAfford(int amount) {
        return balance >= amount;
    }
}
