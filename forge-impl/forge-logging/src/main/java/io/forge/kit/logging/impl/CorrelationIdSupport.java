package io.forge.kit.logging.impl;

import java.util.UUID;

/**
 * Resolves correlation identifiers for inbound HTTP requests.
 */
public final class CorrelationIdSupport
{
    private CorrelationIdSupport()
    {
    }

    /**
     * Returns the trimmed incoming header value, or a new UUID when absent or blank.
     */
    public static String resolveCorrelationId(final String incomingHeader)
    {
        if (incomingHeader == null)
        {
            return UUID.randomUUID().toString();
        }

        final String trimmed = incomingHeader.trim();
        if (trimmed.isEmpty())
        {
            return UUID.randomUUID().toString();
        }

        return trimmed;
    }
}
