package com.mamikos.kostapi.common.ratelimit;

import java.time.Duration;

public interface RateLimiter {

    /**
     * Registers one hit against {@code key}.
     *
     * @return the decision for this request, including how long to wait when denied
     */
    Decision tryConsume(String key, int limit, Duration window);

    record Decision(boolean permitted, long retryAfterSeconds) {
        public static Decision allowed() {
            return new Decision(true, 0);
        }

        public static Decision denied(long retryAfterSeconds) {
            return new Decision(false, Math.max(retryAfterSeconds, 1));
        }
    }
}
