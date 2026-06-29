package io.forge.kit.observability.aws.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.forge.kit.http.aws.AwsSignedHttpRequest;
import io.forge.kit.http.aws.AwsSignedHttpResponse;
import io.forge.kit.http.aws.AwsSigningServiceName;
import io.forge.kit.http.aws.SignedHttpTransport;
import io.forge.kit.observability.api.encode.PrometheusRemoteWriteEncoder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpMethod;

class AmpMetricsExporterTest
{
    @Test
    void pushMetrics_sendsSnappyRemoteWriteToAmp()
    {
        final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.counter("forge_probe_total").increment();
        final AtomicReference<AwsSignedHttpRequest> capturedRequest = new AtomicReference<>();
        final SignedHttpTransport transport = request ->
        {
            capturedRequest.set(request);
            return new AwsSignedHttpResponse(204, new byte[0]);
        };
        final SimpleMeterRegistry metrics = new SimpleMeterRegistry();
        final AmpMetricsExporter exporter = new AmpMetricsExporter(
                                                                   registry,
                                                                   new PrometheusRemoteWriteEncoder(),
                                                                   transport,
                                                                   metrics,
                                                                   "https://aps-workspaces.us-west-2.amazonaws.com/workspaces/ws-123/api/v1/remote_write",
                                                                   "us-west-2");

        exporter.pushMetrics();

        final AwsSignedHttpRequest request = capturedRequest.get();
        assertEquals(SdkHttpMethod.POST, request.method());
        assertEquals(AwsSigningServiceName.APS, request.signingService());
        assertEquals("us-west-2", request.region());
        assertEquals("application/x-protobuf", request.headers().get("Content-Type"));
        assertEquals("snappy", request.headers().get("Content-Encoding"));
        assertTrue(request.body().length > 0);
        assertEquals(1.0, metrics.get("forge.observability.amp.push.success").counter().count());
    }
}
