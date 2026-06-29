package io.forge.kit.logging.impl;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

/**
 * Forwards the active correlation identifier to downstream REST client calls.
 */
@Provider
public final class CorrelationIdClientRequestFilter implements ClientRequestFilter
{
    @Override
    public void filter(final ClientRequestContext clientRequestContext)
    {
        if (correlationIdHeaderNotPresent(clientRequestContext))
        {
            final Object correlationId = MDC.get(CorrelationIdHeaders.MDC_KEY);
            if (correlationId instanceof String id && !id.isBlank())
            {
                clientRequestContext.getHeaders().putSingle(CorrelationIdHeaders.HEADER, id);
            }
        }
    }

    private static boolean correlationIdHeaderNotPresent(ClientRequestContext clientRequestContext)
    {
        return !clientRequestContext.getHeaders().containsKey(CorrelationIdHeaders.HEADER);
    }
}
