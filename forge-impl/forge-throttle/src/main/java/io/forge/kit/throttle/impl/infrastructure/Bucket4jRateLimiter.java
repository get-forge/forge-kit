package io.forge.kit.throttle.impl.infrastructure;

import io.forge.kit.throttle.api.infrastructure.RateLimitStatus;
import io.forge.kit.throttle.api.infrastructure.RateLimiter;
import io.forge.kit.throttle.api.infrastructure.RateLimiterProperties;
import io.github.bucket4j.Bucket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter implementation using Bucket4j.
 *
 * <p>This bean is only created if {@link RateLimiterProperties} is available, allowing
 * services without rate-limit configuration to start successfully.</p>
 *
 * <p>The class is only created via the producer method in {@link Bucket4jRateLimiterProducer}
 * when properties are available. It has no CDI annotations, so it won't be auto-discovered as a bean.</p>
 */
public class Bucket4jRateLimiter implements RateLimiter
{
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final RateLimiterProperties properties;

    // Package-private constructor for producer and testing
    Bucket4jRateLimiter(final RateLimiterProperties properties)
    {
        this.properties = properties;
    }

    @Override
    public RateLimitStatus tryConsume(final String key)
    {
        final Bucket bucket = buckets.computeIfAbsent(key, this::createBucket);
        final long capacity = properties.resolveCapacityForKey(key);
        return RateLimitStatusMapper.fromProbe(bucket.tryConsumeAndReturnRemaining(1L), capacity);
    }

    /**
     * Clears all rate limit buckets. Intended for testing only.
     */
    @Override
    public void resetForTests()
    {
        clearBuckets();
    }

    /**
     * Clears all rate limit buckets. Intended for testing only.
     */
    public void clearBuckets()
    {
        buckets.clear();
    }

    private Bucket createBucket(final String key)
    {
        return Bucket.builder()
            .addLimit(BucketConfigurationFactory.createBandwidth(properties, key))
            .build();
    }
}
