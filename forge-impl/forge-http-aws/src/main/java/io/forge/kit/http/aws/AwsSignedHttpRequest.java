package io.forge.kit.http.aws;

import java.net.URI;
import java.util.Map;
import software.amazon.awssdk.http.SdkHttpMethod;

/**
 * Unsigned HTTP request payload for {@link AwsSignedHttpTransport}.
 */
public record AwsSignedHttpRequest(
                                   SdkHttpMethod method,
                                   URI uri,
                                   Map<String, String> headers,
                                   byte[] body,
                                   AwsSigningServiceName signingService,
                                   String region)
{
    public AwsSignedHttpRequest
    {
        headers = Map.copyOf(headers);
        body = body == null ? new byte[0] : body.clone();
    }

    @Override
    public byte[] body()
    {
        return body.clone();
    }
}
