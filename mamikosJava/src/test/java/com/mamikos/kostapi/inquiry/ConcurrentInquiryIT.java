package com.mamikos.kostapi.inquiry;

import static org.assertj.core.api.Assertions.assertThat;

import com.mamikos.kostapi.auth.web.dto.AuthResponse;
import com.mamikos.kostapi.common.response.ApiResponse;
import com.mamikos.kostapi.credit.entity.CreditTransactionType;
import com.mamikos.kostapi.credit.repository.CreditBalanceRepository;
import com.mamikos.kostapi.credit.repository.CreditTransactionRepository;
import com.mamikos.kostapi.inquiry.web.dto.CreateInquiryRequest;
import com.mamikos.kostapi.kost.web.dto.response.OwnerKostResponse;
import com.mamikos.kostapi.support.AbstractIntegrationTest;
import com.mamikos.kostapi.support.TestApi;
import com.mamikos.kostapi.user.entity.UserRole;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * The one test in this suite that would have caught a naive (read-then-write, no lock)
 * implementation of credit deduction. Ten requests race for a wallet that can only afford
 * four of them; without the pessimistic row lock in
 * {@code CreditBalanceRepository#findByUserIdForUpdate}, several would read the same
 * "still have enough" balance before any of them wrote it back, and the wallet would end up
 * negative.
 */
class ConcurrentInquiryIT extends AbstractIntegrationTest {

    @Autowired
    private CreditBalanceRepository creditBalanceRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Test
    void exactlyFourOfTenConcurrentInquiriesSucceedAgainstATwentyCreditBalance() throws InterruptedException {
        AuthResponse owner = TestApi.register(restTemplate, "Concurrency Owner", UserRole.OWNER);
        OwnerKostResponse kost = TestApi.createKost(
                restTemplate,
                owner.token().accessToken(),
                "Concurrency Kost",
                new BigDecimal("500000"),
                "ConcurrencyCity");
        AuthResponse user = TestApi.register(restTemplate, "Concurrency User", UserRole.REGULAR);
        String userToken = user.token().accessToken();
        Long userId = user.user().id();

        int requestCount = 10;
        CountDownLatch startLine = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger rejectedCount = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(requestCount);

        try {
            List<Future<?>> futures = IntStream.range(0, requestCount)
                    .<Future<?>>mapToObj(i -> pool.submit(() -> {
                        try {
                            startLine.await();
                            var response = restTemplate.exchange(
                                    "/api/v1/kosts/{id}/availability-inquiries",
                                    HttpMethod.POST,
                                    new HttpEntity<>(
                                            new CreateInquiryRequest("concurrent #" + i), TestApi.bearer(userToken)),
                                    ApiResponse.class,
                                    kost.id());
                            if (response.getStatusCode() == HttpStatus.CREATED) {
                                successCount.incrementAndGet();
                            } else {
                                rejectedCount.incrementAndGet();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }))
                    .toList();

            startLine.countDown();
            for (Future<?> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException("Concurrent inquiry request failed", e);
                }
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(successCount.get()).isEqualTo(4);
        assertThat(rejectedCount.get()).isEqualTo(6);

        int finalBalance =
                creditBalanceRepository.findByUserId(userId).orElseThrow().getBalance();
        assertThat(finalBalance).isZero();

        long deductionCount = creditTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .filter(tx -> tx.getType() == CreditTransactionType.INQUIRY_DEDUCTION)
                .count();
        assertThat(deductionCount).isEqualTo(4);
    }
}
