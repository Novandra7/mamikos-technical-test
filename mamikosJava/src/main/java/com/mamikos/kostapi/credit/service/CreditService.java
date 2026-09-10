package com.mamikos.kostapi.credit.service;

import com.mamikos.kostapi.common.exception.CreditWalletNotFoundException;
import com.mamikos.kostapi.credit.entity.CreditBalance;
import com.mamikos.kostapi.credit.entity.CreditTransaction;
import com.mamikos.kostapi.credit.repository.CreditBalanceRepository;
import com.mamikos.kostapi.credit.repository.CreditTransactionRepository;
import com.mamikos.kostapi.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Governs an existing credit wallet: spending it, and reading its balance and history.
 *
 * <p>Locking and ledger-writing are two separate calls rather than one, because the
 * inquiry row the deduction is charged against does not have an id until it is saved, and
 * that has to happen <em>between</em> the lock and the ledger entry. Both calls are
 * {@code MANDATORY}: this class never opens its own transaction, since the lock, the
 * inquiry insert, and the ledger row must all commit or roll back together.
 */
@Service
public class CreditService {

    private final CreditBalanceRepository creditBalanceRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    public CreditService(
            CreditBalanceRepository creditBalanceRepository, CreditTransactionRepository creditTransactionRepository) {
        this.creditBalanceRepository = creditBalanceRepository;
        this.creditTransactionRepository = creditTransactionRepository;
    }

    /**
     * Locks the wallet row with {@code SELECT ... FOR UPDATE} for the rest of the caller's
     * transaction. This is what serialises two concurrent inquiries against the same
     * wallet instead of letting them race each other to a negative balance.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public CreditBalance lockBalance(Long userId) {
        return creditBalanceRepository.findByUserIdForUpdate(userId).orElseThrow(CreditWalletNotFoundException::new);
    }

    /**
     * Appends the ledger row for a deduction the caller already applied to {@code balance}
     * via {@link CreditBalance#deduct}. Kept separate from the deduction itself so the
     * caller can create the entity the deduction is charged against (which needs the
     * deduction to have already been validated) before this ledger row is written.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordDeduction(User user, int amount, int balanceBefore, Long referenceId) {
        creditTransactionRepository.save(CreditTransaction.inquiryDeduction(user, amount, balanceBefore, referenceId));
    }

    @Transactional(readOnly = true)
    public CreditBalance requireBalance(Long userId) {
        return creditBalanceRepository.findByUserId(userId).orElseThrow(CreditWalletNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Page<CreditTransaction> history(Long userId, Pageable pageable) {
        return creditTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
