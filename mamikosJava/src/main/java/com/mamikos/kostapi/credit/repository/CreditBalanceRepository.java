package com.mamikos.kostapi.credit.repository;

import com.mamikos.kostapi.credit.entity.CreditBalance;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface CreditBalanceRepository extends JpaRepository<CreditBalance, Long> {

    Optional<CreditBalance> findByUserId(Long userId);

    /**
     * Locks the wallet row for the duration of the caller's transaction.
     *
     * <p>This is what actually prevents a negative balance under concurrent inquiries: two
     * requests reading the same row before either writes back would otherwise both see
     * "enough credit" and both succeed. The row lock serialises them instead.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select b from CreditBalance b where b.user.id = :userId")
    Optional<CreditBalance> findByUserIdForUpdate(@Param("userId") Long userId);

    @Query("select b from CreditBalance b join fetch b.user u where u.role in ('REGULAR', 'PREMIUM')")
    Slice<CreditBalance> findAllRechargeable(Pageable pageable);
}
