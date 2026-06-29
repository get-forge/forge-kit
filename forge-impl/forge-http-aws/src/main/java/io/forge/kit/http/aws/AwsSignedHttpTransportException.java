package io.forge.kit.http.aws;

/**
 * Raised when a signed HTTP request fails after retries or cannot be signed.
 */
public final class AwsSignedHttpTransportException extends RuntimeException
{
    private final int statusCode;

    public AwsSignedHttpTransportException(final String message)
    {
        super(message);
        this.statusCode = 0;
    }

    public AwsSignedHttpTransportException(final String message, final int statusCode)
    {
        super(message);
        this.statusCode = statusCode;
    }

    public AwsSignedHttpTransportException(final String message, final Throwable cause)
    {
        super(message, cause);
        this.statusCode = 0;
    }

    /**
     * Returns the HTTP status code when the failure was a non-retryable response.
     */
    public int statusCode()
    {
        return statusCode;
    }
}
