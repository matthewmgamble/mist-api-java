package ca.mgamble.mist;

import java.time.Duration;
import java.util.Objects;

/**
 * Controls how the client retries failed requests.
 * <ul>
 *   <li><b>429 Too Many Requests</b> is retried for every method, waiting for {@code Retry-After} (or the
 *       backoff when absent), unless the wait exceeds {@code maxRateLimitWait}.</li>
 *   <li><b>5xx responses and I/O errors</b> are retried with exponential backoff, but only for idempotent
 *       methods (GET, PUT, DELETE), so a POST is never sent twice.</li>
 * </ul>
 *
 * @param maxRetries       retries after the first attempt; 0 disables retrying
 * @param initialBackoff   wait before the first retry; doubles each attempt
 * @param maxBackoff       cap on any single backoff wait
 * @param maxRateLimitWait longest {@code Retry-After} the client will honour before failing fast
 */
public record RetryPolicy(int maxRetries, Duration initialBackoff, Duration maxBackoff, Duration maxRateLimitWait) {

    public RetryPolicy {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0");
        }
        Objects.requireNonNull(initialBackoff, "initialBackoff");
        Objects.requireNonNull(maxBackoff, "maxBackoff");
        Objects.requireNonNull(maxRateLimitWait, "maxRateLimitWait");
    }

    /** 3 retries, 1s initial backoff capped at 30s, honouring {@code Retry-After} up to 60s. */
    public static RetryPolicy defaults() {
        return new RetryPolicy(3, Duration.ofSeconds(1), Duration.ofSeconds(30), Duration.ofSeconds(60));
    }

    public static RetryPolicy none() {
        return new RetryPolicy(0, Duration.ZERO, Duration.ZERO, Duration.ZERO);
    }

    Duration backoff(int attempt) {
        long millis = initialBackoff.toMillis() << Math.min(attempt, 20);
        return Duration.ofMillis(Math.min(millis, maxBackoff.toMillis()));
    }
}
