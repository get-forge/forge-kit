package io.forge.kit.http.aws;

/**
 * HTTP response returned by {@link AwsSignedHttpTransport}.
 */
public record AwsSignedHttpResponse(int statusCode, byte[] body)
{
    public AwsSignedHttpResponse
    {
        body = body == null ? new byte[0] : body.clone();
    }

    @Override
    public byte[] body()
    {
        return body.clone();
    }

    /**
     * Returns whether the status code indicates success.
     */
    public boolean isSuccessful()
    { return statusCode >= 200 && statusCode < 300; }
}
