package io.forge.kit.logging.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class CorrelationIdSupportTest
{
    @Test
    void resolveCorrelationIdUsesIncomingHeaderWhenPresent()
    {
        assertEquals("abc-123", CorrelationIdSupport.resolveCorrelationId("abc-123"));
        assertEquals("abc-123", CorrelationIdSupport.resolveCorrelationId("  abc-123  "));
    }

    @Test
    void resolveCorrelationIdGeneratesUuidWhenMissing()
    {
        final String generated = CorrelationIdSupport.resolveCorrelationId(null);
        assertFalse(generated.isBlank());
        assertNotEquals(CorrelationIdSupport.resolveCorrelationId(null), generated);
    }

    @Test
    void resolveCorrelationIdGeneratesUuidWhenBlank()
    {
        final String generated = CorrelationIdSupport.resolveCorrelationId("   ");
        assertFalse(generated.isBlank());
    }
}
