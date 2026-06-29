package io.forge.kit.logging.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redacts common secret and PII patterns from log message text.
 */
public final class SensitiveLogMasker
{
    private static final String REDACTED = "[REDACTED]";

    private static final String REDACTED_EMAIL = "[REDACTED_EMAIL]";

    private static final Pattern BEARER_TOKEN = Pattern.compile(
        "Bearer\\s+\\S+",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern JWT_TOKEN = Pattern.compile(
        "eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"
    );

    private static final Pattern EMAIL = Pattern.compile(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
    );

    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
        "(?i)(password|passwd|secret|api[_-]?key|authorization)(\\s*[:=]\\s*)\\S+"
    );

    private static final Pattern AWS_ACCESS_KEY = Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b");

    private SensitiveLogMasker()
    {
    }

    /**
     * Returns {@code text} with sensitive values replaced by redaction placeholders.
     */
    public static String mask(final String text)
    {
        if (text == null || text.isEmpty())
        {
            return text;
        }

        String masked = text;
        masked = replaceAll(masked, BEARER_TOKEN, "Bearer " + REDACTED);
        masked = replaceAll(masked, JWT_TOKEN, REDACTED);
        masked = replaceAll(masked, EMAIL, REDACTED_EMAIL);
        masked = maskSecretAssignments(masked);
        masked = replaceAll(masked, AWS_ACCESS_KEY, REDACTED);
        return masked;
    }

    private static String maskSecretAssignments(final String input)
    {
        final Matcher matcher = SECRET_ASSIGNMENT.matcher(input);
        final StringBuilder builder = new StringBuilder();
        while (matcher.find())
        {
            matcher.appendReplacement(
                builder,
                Matcher.quoteReplacement(matcher.group(1) + matcher.group(2) + REDACTED)
            );
        }

        matcher.appendTail(builder);
        return builder.toString();
    }

    /**
     * Returns a copy of {@code parameters} with each string element masked.
     */
    public static Object[] maskParameters(final Object[] parameters)
    {
        if (parameters == null || parameters.length == 0)
        {
            return parameters;
        }

        final Object[] masked = parameters.clone();
        for (int index = 0; index < masked.length; index++)
        {
            masked[index] = maskParameter(masked[index]);
        }

        return masked;
    }

    private static Object maskParameter(final Object parameter)
    {
        if (parameter instanceof String value)
        {
            return mask(value);
        }

        return parameter;
    }

    private static String replaceAll(final String input, final Pattern pattern, final String replacement)
    {
        return pattern.matcher(input).replaceAll(Matcher.quoteReplacement(replacement));
    }
}
