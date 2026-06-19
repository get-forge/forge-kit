package io.forge.kit.throttle.impl.infrastructure;

import io.forge.kit.throttle.api.infrastructure.RateLimitStatus;
import io.forge.kit.throttle.api.infrastructure.RateLimiter;
import io.forge.kit.throttle.api.infrastructure.RateLimiterProperties;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.vertx.Bucket4jVertx;
import io.vertx.redis.client.Redis;
import java.time.Duration;
import java.util.Objects;

/**
 * Distributed rate limiter backed by Redis via Bucket4j Vert.x integration.
 *
 * <p>Bucket keys are stored in Redis with the {@code ratelimit:} prefix, separate from cache keys.</p>
 */
public class RedisBucket4jRateLimiter implements RateLimiter
{
    static final String KEY_PREFIX = "ratelimit:";

    private static final Duration BUCKET_EXPIRATION = Duration.ofHours(1L);

    private final RateLimiterProperties properties;
    private final ProxyManager<String> proxyManager;

    RedisBucket4jRateLimiter(final RateLimiterProperties properties, final io.vertx.mutiny.redis.client.Redis mutinyRedis)
    {
        this.properties = Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(mutinyRedis, "mutinyRedis");

        final Redis redis = new VertxMutinyRedisBridge(mutinyRedis);
        this.proxyManager = Bucket4jVertx.casBasedBuilder(redis)
            .keyMapper(Mapper.STRING)
            .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(BUCKET_EXPIRATION))
            .build();
    }

    @Override
    public RateLimitStatus tryConsume(final String key)
    {
        final String redisKey = KEY_PREFIX + key;
        final long capacity = properties.resolveCapacityForKey(key);
        final Bucket bucket = proxyManager.builder()
            .build(redisKey, () -> BucketConfigurationFactory.createConfiguration(properties, key));

        return RateLimitStatusMapper.fromProbe(bucket.tryConsumeAndReturnRemaining(1L), capacity);
    }
}
