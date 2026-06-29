package io.forge.kit.logging.impl;

import io.quarkus.logging.LoggingFilter;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import org.jboss.logmanager.ExtLogRecord;

/**
 * Masks secrets and PII in log records before they are written to the console handler.
 */
@LoggingFilter(name = SensitiveDataLogFilter.FILTER_NAME)
public final class SensitiveDataLogFilter implements Filter
{
    public static final String FILTER_NAME = "sensitive-data-mask";

    @Override
    public boolean isLoggable(final LogRecord record)
    {
        final String formattedMessage = resolveFormattedMessage(record);
        if (formattedMessage != null)
        {
            record.setMessage(SensitiveLogMasker.mask(formattedMessage));
            record.setParameters(null);
        }

        return true;
    }

    private static String resolveFormattedMessage(final LogRecord record)
    {
        if (record instanceof ExtLogRecord extRecord)
        {
            return extRecord.getFormattedMessage();
        }

        if (record.getParameters() == null || record.getParameters().length == 0)
        {
            return record.getMessage();
        }

        return String.format(record.getMessage(), record.getParameters());
    }
}
