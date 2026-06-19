package io.forge.kit.throttle.impl.infrastructure.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.forge.kit.throttle.impl.infrastructure.Bucket4jRateLimiterProducer;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.Response;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RedisRateLimitHealthCheckTest
{
    @Test
    @DisplayName("Returns up when rate limit store is memory")
    void returnsUpWhenStoreIsMemory()
    {
        final RedisRateLimitHealthCheck healthCheck = new RedisRateLimitHealthCheck(null, Bucket4jRateLimiterProducer.MEMORY_STORE)
        {
        };

        final HealthCheckResponse response = healthCheck.call();

        assertEquals(RedisRateLimitHealthCheck.CHECK_NAME, response.getName());
        assertSame(HealthCheckResponse.Status.UP, response.getStatus());
        assertEquals(Bucket4jRateLimiterProducer.MEMORY_STORE, response.getData().orElseThrow().get("store"));
    }

    @Test
    @DisplayName("Returns down when redis store is configured without a redis client")
    void returnsDownWhenRedisStoreWithoutClient()
    {
        final RedisRateLimitHealthCheck healthCheck = new RedisRateLimitHealthCheck(null, Bucket4jRateLimiterProducer.REDIS_STORE)
        {
        };

        final HealthCheckResponse response = healthCheck.call();

        assertSame(HealthCheckResponse.Status.DOWN, response.getStatus());
        assertEquals("Redis client not available", response.getData().orElseThrow().get("reason"));
    }

    @Test
    @DisplayName("Returns up when redis store pings successfully")
    void returnsUpWhenRedisPingSucceeds()
    {
        final Redis redis = mock(Redis.class);
        when(redis.send(any())).thenReturn(Uni.createFrom().item(mock(Response.class)));

        final RedisRateLimitHealthCheck healthCheck = new RedisRateLimitHealthCheck(redis, Bucket4jRateLimiterProducer.REDIS_STORE)
        {
        };

        final HealthCheckResponse response = healthCheck.call();

        assertSame(HealthCheckResponse.Status.UP, response.getStatus());
        verify(redis).send(any());
    }

    @Test
    @DisplayName("Returns down when redis ping fails")
    void returnsDownWhenRedisPingFails()
    {
        final Redis redis = mock(Redis.class);
        when(redis.send(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("connection refused")));

        final RedisRateLimitHealthCheck healthCheck = new RedisRateLimitHealthCheck(redis, Bucket4jRateLimiterProducer.REDIS_STORE)
        {
        };

        final HealthCheckResponse response = healthCheck.call();

        assertSame(HealthCheckResponse.Status.DOWN, response.getStatus());
        assertEquals("connection refused", response.getData().orElseThrow().get("error"));
    }
}
