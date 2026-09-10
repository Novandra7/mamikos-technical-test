package com.mamikos.kostapi.credit.service;

import com.mamikos.kostapi.credit.config.CreditProperties;
import com.mamikos.kostapi.credit.entity.CreditBalance;
import com.mamikos.kostapi.credit.entity.CreditTransaction;
import com.mamikos.kostapi.credit.repository.CreditBalanceRepository;
import com.mamikos.kostapi.credit.repository.CreditTransactionRepository;
import com.mamikos.kostapi.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opens a new user's credit wallet at registration time.
 *
 * <p>Kept separate from {@code CreditService} (which governs an existing wallet's balance)
 * because "open a wallet" and "spend or refill a wallet" are different lifecycles: this one
 * runs exactly once, right after a user row is inserted.
 */
@Service
public class CreditGrantService {

    private final CreditBalanceRepository creditBalanceRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final CreditProperties creditProperties;

    public CreditGrantService(
            CreditBalanceRepository creditBalanceRepository,
            CreditTransactionRepository creditTransactionRepository,
            CreditProperties creditProperties) {
        this.creditBalanceRepository = creditBalanceRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.creditProperties = creditProperties;
    }

    /**
     * Owners never receive a wallet — "no credit" is the absence of one, not a zero
     * balance — so this is a no-op for them by design, not an oversight.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void grantInitialCreditIfApplicable(User user) {
        if (!user.hasCreditWallet()) {
            return;
        }
        int quota = creditProperties.quotaFor(user.getRole());
        CreditBalance balance = CreditBalance.openFor(user, quota);
        creditBalanceRepository.save(balance);
        creditTransactionRepository.save(CreditTransaction.initialGrant(user, quota));
    }
}
