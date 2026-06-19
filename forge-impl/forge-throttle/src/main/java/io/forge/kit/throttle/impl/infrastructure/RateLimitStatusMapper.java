package io.forge.kit.throttle.impl.infrastructure;

import io.forge.kit.throttle.api.infrastructure.RateLimitStatus;
import io.github.bucket4j.ConsumptionProbe;

/**
 * Maps Bucket4j consumption probes to rate limit status responses.
 */
final class RateLimitStatusMapper
{
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private RateLimitStatusMapper()
    {
    }

    static RateLimitStatus fromProbe(final ConsumptionProbe probe, final long capacity)
    {
        if (probe.isConsumed())
        {
            return new RateLimitStatus(true, capacity, probe.getRemainingTokens(), 0L);
        }

        final long retryAfterNanos = probe.getNanosToWaitForRefill();
        final long retryAfterSeconds = retryAfterNanos > 0L ? (retryAfterNanos / NANOS_PER_SECOND) + 1L : 1L;
        return new RateLimitStatus(false, capacity, probe.getRemainingTokens(), retryAfterSeconds);
    }
}
