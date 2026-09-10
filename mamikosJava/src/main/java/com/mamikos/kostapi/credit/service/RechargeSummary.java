package com.mamikos.kostapi.credit.service;

/** Result of one recharge run, logged and returned to whatever triggered it (scheduler,
 * manual job runner, or a test). */
public record RechargeSummary(int processed, int recharged, int skipped, int failed, long durationMillis) {

    public static RechargeSummary empty() {
        return new RechargeSummary(0, 0, 0, 0, 0);
    }

    public RechargeSummary merge(RechargeSummary other) {
        return new RechargeSummary(
                processed + other.processed,
                recharged + other.recharged,
                skipped + other.skipped,
                failed + other.failed,
                durationMillis + other.durationMillis);
    }

    public RechargeSummary withDuration(long millis) {
        return new RechargeSummary(processed, recharged, skipped, failed, millis);
    }
}
