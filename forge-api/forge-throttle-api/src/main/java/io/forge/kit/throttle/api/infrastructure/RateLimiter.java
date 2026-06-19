package io.forge.kit.throttle.api.infrastructure;

/**
 * Contract for rate limiters used across services.
 */
public interface RateLimiter
{
    /**
     * Attempts to consume a single permit for the given key.
     *
     * @param key unique identifier for the rate limit bucket
     * @return status of the rate limit decision
     */
    RateLimitStatus tryConsume(String key);

    /**
     * Clears in-memory or Redis bucket state. Intended for integration tests only.
     */
    default void resetForTests()
    {
    }
}
