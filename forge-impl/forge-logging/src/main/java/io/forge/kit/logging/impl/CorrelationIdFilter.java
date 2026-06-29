package io.forge.kit.logging.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.MDC;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

/**
 * Propagates a correlation identifier through MDC and the {@code X-Correlation-Id} response header.
 */
@ApplicationScoped
public final class CorrelationIdFilter
{
    @ServerRequestFilter(preMatching = true, priority = Priorities.HEADER_DECORATOR)
    public void addCorrelationId(final ResteasyReactiveContainerRequestContext requestContext)
    {
        final String correlationId = CorrelationIdSupport.resolveCorrelationId(requestContext.getHeaderString(CorrelationIdHeaders.HEADER));
        MDC.put(CorrelationIdHeaders.MDC_KEY, correlationId);
    }

    @ServerResponseFilter(priority = Priorities.HEADER_DECORATOR)
    public void echoCorrelationId(final ResteasyReactiveContainerRequestContext ignoredRequestContext, final ContainerResponseContext responseContext)
    {
        final String correlationId = (String) MDC.get(CorrelationIdHeaders.MDC_KEY);
        if (correlationId != null)
        {
            final MultivaluedMap<String, Object> headers = responseContext.getHeaders();
            headers.putSingle(CorrelationIdHeaders.HEADER, correlationId);
        }

        MDC.remove(CorrelationIdHeaders.MDC_KEY);
    }
}
