package io.forge.kit.http.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

class AwsSignedHttpTransportTest
{
    private static final String REGION = "us-west-2";
    private static final byte[] REQUEST_BODY = "metrics-payload".getBytes(StandardCharsets.UTF_8);

    private HttpServer httpServer;
    private final List<String> authorizationHeaders = new CopyOnWriteArrayList<>();
    private final AtomicInteger requestCount = new AtomicInteger();

    private AwsSignedHttpTransport transport;
    private URI endpoint;

    @BeforeEach
    void setUp() throws IOException
    {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/remote_write", exchange ->
        {
            requestCount.incrementAndGet();
            authorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        httpServer.start();
        endpoint = URI.create("http://localhost:" + httpServer.getAddress().getPort() + "/remote_write");
        transport = new AwsSignedHttpTransport(
                                               StaticCredentialsProvider.create(AwsBasicCredentials.create("access-key", "secret-key")),
                                               UrlConnectionHttpClient.create(),
                                               AwsV4HttpSigner.create());
    }

    @AfterEach
    void tearDown()
    {
        httpServer.stop(0);
    }

    @Test
    void send_AddsSigV4AuthorizationHeader()
    {
        final AwsSignedHttpResponse response = transport.send(sampleRequest());

        assertTrue(response.isSuccessful());
        assertEquals(1, requestCount.get());
        assertNotNull(authorizationHeaders.getFirst());
        assertTrue(authorizationHeaders.getFirst().startsWith("AWS4-HMAC-SHA256"));
    }

    @Test
    void send_RetriesRetryableResponses()
    {
        httpServer.removeContext("/remote_write");
        final AtomicInteger attempts = new AtomicInteger();
        httpServer.createContext("/remote_write", exchange ->
        {
            final int attempt = attempts.incrementAndGet();
            if (attempt < 3)
            {
                exchange.sendResponseHeaders(503, -1);
            }
            else
            {
                exchange.sendResponseHeaders(204, -1);
            }
            exchange.close();
        });

        final AwsSignedHttpResponse response = transport.send(sampleRequest());

        assertEquals(204, response.statusCode());
        assertEquals(3, attempts.get());
    }

    private AwsSignedHttpRequest sampleRequest()
    {
        return new AwsSignedHttpRequest(
                                        SdkHttpMethod.POST,
                                        endpoint,
                                        Map.of("Content-Type", "application/x-protobuf"),
                                        REQUEST_BODY,
                                        AwsSigningServiceName.APS,
                                        REGION);
    }
}
