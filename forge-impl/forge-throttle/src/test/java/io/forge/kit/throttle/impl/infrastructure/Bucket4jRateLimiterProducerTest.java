package io.forge.kit.throttle.impl.infrastructure;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.forge.kit.throttle.api.infrastructure.RateLimiter;
import io.forge.kit.throttle.api.infrastructure.RateLimiterProperties;
import io.vertx.mutiny.redis.client.Redis;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Bucket4jRateLimiterProducerTest
{
    @Test
    @DisplayName("Produces in-memory rate limiter when properties are available")
    void producesInMemoryRateLimiterWhenPropertiesAvailable()
    {
        final RateLimiterProperties properties = mock(RateLimiterProperties.class);
        final Instance<RateLimiterProperties> propertiesInstance = mock(Instance.class);
        final Instance<Redis> redisInstance = mock(Instance.class);

        when(propertiesInstance.isResolvable()).thenReturn(true);
        when(propertiesInstance.get()).thenReturn(properties);

        final Bucket4jRateLimiterProducer producer = new Bucket4jRateLimiterProducer();
        final RateLimiter result = producer.produceRateLimiter(propertiesInstance, redisInstance);

        assertNotNull(result);
        assertInstanceOf(Bucket4jRateLimiter.class, result);
    }

    @Test
    @DisplayName("Returns null when properties are not available")
    void returnsNullWhenPropertiesNotAvailable()
    {
        final Instance<RateLimiterProperties> propertiesInstance = mock(Instance.class);
        final Instance<Redis> redisInstance = mock(Instance.class);

        when(propertiesInstance.isResolvable()).thenReturn(false);

        final Bucket4jRateLimiterProducer producer = new Bucket4jRateLimiterProducer();
        final RateLimiter result = producer.produceRateLimiter(propertiesInstance, redisInstance);

        assertNull(result);
    }
}
