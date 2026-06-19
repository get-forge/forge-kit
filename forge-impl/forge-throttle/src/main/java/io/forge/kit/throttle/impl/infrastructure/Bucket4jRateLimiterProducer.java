package io.forge.kit.throttle.impl.infrastructure;

import io.forge.kit.throttle.api.infrastructure.RateLimiter;
import io.forge.kit.throttle.api.infrastructure.RateLimiterProperties;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

/**
 * Conditional producer for {@link RateLimiter} implementations.
 *
 * <p>Selects in-memory ({@code memory}) or distributed ({@code redis}) storage based on
 * {@code rate-limit.store}. Defaults to in-memory when the property is absent.</p>
 */
@ApplicationScoped
public class Bucket4jRateLimiterProducer
{
    private static final Logger LOGGER = Logger.getLogger(Bucket4jRateLimiterProducer.class);

    public static final String STORE_PROPERTY = "rate-limit.store";

    public static final String MEMORY_STORE = "memory";

    public static final String REDIS_STORE = "redis";

    @Produces
    @ApplicationScoped
    public RateLimiter produceRateLimiter(
                                          final Instance<RateLimiterProperties> propertiesInstance, final Instance<io.vertx.mutiny.redis.client.Redis> redisInstance
    )
    {
        if (!propertiesInstance.isResolvable())
        {
            LOGGER.debug("RateLimiterProperties not available - RateLimiter will not be created");
            return null;
        }

        final RateLimiterProperties properties = propertiesInstance.get();
        final Config config = ConfigProvider.getConfig();
        final String store = config.getOptionalValue(STORE_PROPERTY, String.class).orElse(MEMORY_STORE);

        if (REDIS_STORE.equals(store))
        {
            if (!redisInstance.isResolvable())
            {
                throw new IllegalStateException(
                                                "rate-limit.store=redis requires quarkus-redis-client and quarkus.redis.hosts configuration"
                );
            }

            LOGGER.debug("Creating Redis-backed rate limiter");
            return new RedisBucket4jRateLimiter(properties, redisInstance.get());
        }

        LOGGER.debug("Creating in-memory rate limiter");
        return new Bucket4jRateLimiter(properties);
    }
}
