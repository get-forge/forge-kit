package io.forge.kit.throttle.impl.infrastructure.health;

import io.forge.kit.throttle.impl.infrastructure.Bucket4jRateLimiterProducer;
import io.vertx.mutiny.redis.client.Redis;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.Readiness;

/**
 * Registers the Redis rate-limit readiness check for services using forge-throttle.
 */
@ApplicationScoped
public class RateLimitHealthCheckProducer
{
    @Produces
    @Readiness
    @ApplicationScoped
    HealthCheck redisRateLimitHealthCheck(final Instance<Redis> redisInstance)
    {
        final Config config = ConfigProvider.getConfig();
        final String store = config.getOptionalValue(Bucket4jRateLimiterProducer.STORE_PROPERTY, String.class)
            .orElse(Bucket4jRateLimiterProducer.MEMORY_STORE);

        Redis redis = null;
        if (Bucket4jRateLimiterProducer.REDIS_STORE.equals(store) && redisInstance.isResolvable())
        {
            redis = redisInstance.get();
        }

        return new RedisRateLimitHealthCheck(redis, store)
        {
        };
    }
}
