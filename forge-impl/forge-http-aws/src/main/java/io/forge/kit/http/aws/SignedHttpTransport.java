package io.forge.kit.http.aws;

/**
 * Sends AWS SigV4-signed HTTP requests.
 */
public interface SignedHttpTransport
{
    /**
     * Sends a signed HTTP request with retries on transient failures.
     */
    AwsSignedHttpResponse send(AwsSignedHttpRequest request);
}
