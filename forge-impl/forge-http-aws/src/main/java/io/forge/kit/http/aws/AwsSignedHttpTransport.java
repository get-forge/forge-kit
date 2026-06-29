package io.forge.kit.http.aws;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Set;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Signs outbound HTTP requests with AWS SigV4 and sends them via the URL-connection client.
 *
 * <p>PMD class cyclomatic complexity is suppressed: this class coordinates signing, retries, HTTP
 * execution, and response handling; individual methods remain intentionally small.
 */
@SuppressWarnings("PMD.CyclomaticComplexity")
@ApplicationScoped
public final class AwsSignedHttpTransport implements SignedHttpTransport
{
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(200);
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 500, 502, 503, 504);

    private final AwsCredentialsProvider credentialsProvider;
    private final SdkHttpClient httpClient;
    private final AwsV4HttpSigner signer;

    public AwsSignedHttpTransport()
    {
        this(DefaultCredentialsProvider.create(), UrlConnectionHttpClient.create(), AwsV4HttpSigner.create());
    }

    AwsSignedHttpTransport(
                           final AwsCredentialsProvider credentialsProvider, final SdkHttpClient httpClient, final AwsV4HttpSigner signer)
    {
        this.credentialsProvider = credentialsProvider;
        this.httpClient = httpClient;
        this.signer = signer;
    }

    /**
     * Sends a signed HTTP request with exponential backoff on retryable failures.
     */
    @Override
    public AwsSignedHttpResponse send(final AwsSignedHttpRequest request)
    {
        Duration backoff = INITIAL_BACKOFF;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++)
        {
            final AwsSignedHttpResponse response = executeOnce(request);
            if (response.isSuccessful() || !isRetryable(response.statusCode()) || attempt == MAX_ATTEMPTS)
            {
                return response;
            }
            sleep(backoff);
            backoff = backoff.multipliedBy(2);
        }
        throw new AwsSignedHttpTransportException("Signed HTTP request exhausted retries");
    }

    private AwsSignedHttpResponse executeOnce(final AwsSignedHttpRequest request)
    {
        try
        {
            final var signedRequest = signRequest(request);
            final HttpExecuteRequest executeRequest = HttpExecuteRequest.builder()
                .request(signedRequest.request())
                .contentStreamProvider(signedRequest.payload().orElse(null))
                .build();
            final HttpExecuteResponse executeResponse = httpClient.prepareRequest(executeRequest).call();
            return new AwsSignedHttpResponse(
                                             executeResponse.httpResponse().statusCode(),
                                             readResponseBody(executeResponse));
        }
        catch (final IOException exception)
        {
            throw new AwsSignedHttpTransportException("Signed HTTP request failed", exception);
        }
    }

    private software.amazon.awssdk.http.auth.spi.signer.SignedRequest signRequest(final AwsSignedHttpRequest request)
    {
        return signer.sign(signing -> signing
            .identity(AwsCredentialsIdentityConverter.toIdentity(credentialsProvider.resolveCredentials()))
            .request(buildSdkHttpRequest(request))
            .payload(requestPayload(request))
            .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, request.signingService().signingName())
            .putProperty(AwsV4HttpSigner.REGION_NAME, request.region()));
    }

    private static SdkHttpFullRequest buildSdkHttpRequest(final AwsSignedHttpRequest request)
    {
        final SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
            .method(request.method())
            .uri(request.uri());
        request.headers().forEach(builder::putHeader);
        return builder.build();
    }

    private static ContentStreamProvider requestPayload(final AwsSignedHttpRequest request)
    {
        return request.body().length == 0 ? null : ContentStreamProvider.fromByteArray(request.body());
    }

    private static byte[] readResponseBody(final HttpExecuteResponse executeResponse) throws IOException
    {
        if (!executeResponse.responseBody().isPresent())
        {
            return new byte[0];
        }
        try (InputStream inputStream = executeResponse.responseBody().get())
        {
            return IoUtils.toByteArray(inputStream);
        }
    }

    private static boolean isRetryable(final int statusCode)
    {
        return RETRYABLE_STATUS_CODES.contains(statusCode);
    }

    private static void sleep(final Duration backoff)
    {
        try
        {
            Thread.sleep(backoff.toMillis());
        }
        catch (final InterruptedException exception)
        {
            Thread.currentThread().interrupt();
            throw new AwsSignedHttpTransportException("Signed HTTP retry interrupted", exception);
        }
    }
}
