package com.mamikos.kostapi.inquiry.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifies the owner that a new inquiry has arrived on one of their kosts.
 *
 * <p>Listening {@code AFTER_COMMIT} rather than as a plain {@code @EventListener} matters:
 * if the inquiry's transaction later rolled back (say, the ledger write failed), a listener
 * running synchronously inside that transaction would already have fired a notification for
 * an inquiry that never actually happened.
 *
 * <p>Only logs today — swapping in a real channel (email, push, in-app) means adding a
 * sender here, not touching the transaction that raises the event.
 */
@Component
public class NotifyOwnerListener {

    private static final Logger log = LoggerFactory.getLogger(NotifyOwnerListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInquiryCreated(AvailabilityInquiryCreatedEvent event) {
        log.info(
                "notification.owner.new-inquiry ownerId={} kostId={} inquiryId={} userId={}",
                event.ownerId(),
                event.kostId(),
                event.inquiryId(),
                event.userId());
    }
}
