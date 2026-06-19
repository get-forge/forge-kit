package io.forge.kit.throttle.impl.infrastructure.health;

import io.forge.kit.throttle.impl.infrastructure.Bucket4jRateLimiterProducer;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.Request;
import java.time.Duration;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

/**
 * Readiness check for Redis-backed rate limiting.
 *
 * <p>When {@code rate-limit.store=redis}, readiness fails if Redis is unreachable.
 * Unlike cache, rate limiting does not fail open when Redis is down.</p>
 *
 * <p>Registered via {@link RateLimitHealthCheckProducer} when forge-throttle is on the classpath.</p>
 */
public abstract class RedisRateLimitHealthCheck implements HealthCheck
{
    static final String CHECK_NAME = "rate-limit-redis";

    private static final Duration PING_TIMEOUT = Duration.ofSeconds(2L);

    private final Redis redis;

    private final String store;

    protected RedisRateLimitHealthCheck(final Redis redis, final String store)
    {
        this.redis = redis;
        this.store = store;
    }

    @Override
    public HealthCheckResponse call()
    {
        if (!Bucket4jRateLimiterProducer.REDIS_STORE.equals(store))
        {
            return HealthCheckResponse.named(CHECK_NAME)
                .up()
                .withData("store", store)
                .build();
        }

        if (redis == null)
        {
            return HealthCheckResponse.named(CHECK_NAME)
                .down()
                .withData("store", store)
                .withData("reason", "Redis client not available")
                .build();
        }

        try
        {
            redis.send(Request.cmd(Command.PING))
                .await()
                .atMost(PING_TIMEOUT);

            return HealthCheckResponse.named(CHECK_NAME)
                .up()
                .withData("store", store)
                .build();
        }
        catch (final Exception exception)
        {
            return HealthCheckResponse.named(CHECK_NAME)
                .down()
                .withData("store", store)
                .withData("error", exception.getMessage())
                .build();
        }
    }
}
