package io.forge.kit.logging.impl;

/**
 * HTTP header and MDC keys for request correlation identifiers.
 */
public final class CorrelationIdHeaders
{
    public static final String HEADER = "X-Correlation-Id";

    public static final String MDC_KEY = "correlationId";

    private CorrelationIdHeaders()
    {
    }
}
