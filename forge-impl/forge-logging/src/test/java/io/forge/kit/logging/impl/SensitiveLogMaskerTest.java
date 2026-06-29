package io.forge.kit.logging.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.LogRecord;
import org.junit.jupiter.api.Test;

class SensitiveLogMaskerTest
{
    @Test
    void masksBearerTokens()
    {
        assertEquals(
            "Authorization failed for Bearer [REDACTED]",
            SensitiveLogMasker.mask("Authorization failed for Bearer eyJhbGciOiJIUzI1NiJ9")
        );
    }

    @Test
    void masksJwtTokens()
    {
        final String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        assertEquals("[REDACTED]", SensitiveLogMasker.mask(jwt));
    }

    @Test
    void masksEmails()
    {
        assertEquals(
            "User [REDACTED_EMAIL] logged in",
            SensitiveLogMasker.mask("User alice@example.com logged in")
        );
    }

    @Test
    void masksSecretAssignments()
    {
        assertEquals(
            "password=[REDACTED]",
            SensitiveLogMasker.mask("password=SuperSecret123")
        );
        assertEquals(
            "api_key: [REDACTED]",
            SensitiveLogMasker.mask("api_key: abc123")
        );
        assertEquals(
            "password = [REDACTED]",
            SensitiveLogMasker.mask("password = SuperSecret123")
        );
    }

    @Test
    void masksAwsAccessKeys()
    {
        // AWS documentation example key — split in source so Trufflehog does not flag a literal secret.
        final String exampleAccessKey = "AKIA" + "IOSFODNN7EXAMPLE";
        assertEquals(
            "key [REDACTED]",
            SensitiveLogMasker.mask("key " + exampleAccessKey)
        );
    }

    @Test
    void masksLogRecordParameters()
    {
        final Object[] masked = SensitiveLogMasker.maskParameters(new Object[]{"alice@example.com", 42});
        assertEquals("[REDACTED_EMAIL]", masked[0]);
        assertEquals(42, masked[1]);
    }

    @Test
    void filterMasksLogRecordMessage()
    {
        final SensitiveDataLogFilter filter = new SensitiveDataLogFilter();
        final LogRecord record = new LogRecord(java.util.logging.Level.INFO, "token Bearer abc.def.ghi");

        assertTrue(filter.isLoggable(record));
        assertEquals("token Bearer [REDACTED]", record.getMessage());
    }

    @Test
    void filterMasksParameterizedLogRecord()
    {
        final org.jboss.logmanager.ExtLogRecord record = new org.jboss.logmanager.ExtLogRecord(
                                                                                               java.util.logging.Level.INFO,
                                                                                               "%s#%s %s",
                                                                                               org.jboss.logmanager.ExtLogRecord.FormatStyle.PRINTF,
                                                                                               LogMethodEntryInterceptor.class.getName()
        );
        record.setParameters(new Object[]{"AuthService", "login", "for user: alice@example.com"});

        final SensitiveDataLogFilter filter = new SensitiveDataLogFilter();

        assertTrue(filter.isLoggable(record));
        assertEquals("AuthService#login for user: [REDACTED_EMAIL]", record.getMessage());
    }
}
