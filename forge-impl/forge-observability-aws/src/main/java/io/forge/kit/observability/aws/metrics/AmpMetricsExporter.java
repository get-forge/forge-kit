package io.forge.kit.observability.aws.metrics;

import io.forge.kit.http.aws.AwsSignedHttpRequest;
import io.forge.kit.http.aws.AwsSignedHttpResponse;
import io.forge.kit.http.aws.AwsSigningServiceName;
import io.forge.kit.http.aws.SignedHttpTransport;
import io.forge.kit.observability.api.encode.PrometheusRemoteWriteEncoder;
import io.forge.kit.observability.api.encode.RemoteWritePayload;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.http.SdkHttpMethod;

/**
 * Pushes Micrometer Prometheus metrics to Amazon Managed Prometheus via remote write.
 */
@LookupIfProperty(name = "forge.observability.amp.remote-write.enabled", stringValue = "true")
@ApplicationScoped
public final class AmpMetricsExporter
{
    private static final Logger LOGGER = Logger.getLogger(AmpMetricsExporter.class);
    private static final String CONTENT_ENCODING_HEADER = "Content-Encoding";
    private static final String REMOTE_WRITE_VERSION_HEADER = "X-Prometheus-Remote-Write-Version";
    private static final String SNAPPY_ENCODING = "snappy";

    @Inject
    PrometheusMeterRegistry prometheusMeterRegistry;

    @Inject
    PrometheusRemoteWriteEncoder encoder;

    @Inject
    SignedHttpTransport transport;

    @Inject
    MeterRegistry meterRegistry;

    @ConfigProperty(name = "forge.observability.amp.remote-write.url")
    String remoteWriteUrl;

    @ConfigProperty(name = "aws.region")
    String awsRegion;

    AmpMetricsExporter()
    {
    }

    AmpMetricsExporter(
                       final PrometheusMeterRegistry prometheusMeterRegistry, final PrometheusRemoteWriteEncoder encoder, final SignedHttpTransport transport, final MeterRegistry meterRegistry, final String remoteWriteUrl, final String awsRegion)
    {
        this.prometheusMeterRegistry = prometheusMeterRegistry;
        this.encoder = encoder;
        this.transport = transport;
        this.meterRegistry = meterRegistry;
        this.remoteWriteUrl = remoteWriteUrl;
        this.awsRegion = awsRegion;
    }

    /**
     * Pushes the current metric snapshot to AMP on a fixed interval.
     */
    @Scheduled(every = "${forge.observability.amp.push-interval}")
    void pushMetrics()
    {
        final MetricSnapshots snapshots = prometheusMeterRegistry.getPrometheusRegistry().scrape();
        final RemoteWritePayload payload = encoder.encodeSnappy(snapshots);
        final AwsSignedHttpResponse response = transport.send(buildRequest(payload));
        if (response.isSuccessful())
        {
            meterRegistry.counter("forge.observability.amp.push.success").increment();
            return;
        }
        meterRegistry.counter("forge.observability.amp.push.failure").increment();
        LOGGER.errorf("AMP remote write failed with status %d", response.statusCode());
    }

    private AwsSignedHttpRequest buildRequest(final RemoteWritePayload payload)
    {
        return new AwsSignedHttpRequest(
                                        SdkHttpMethod.POST,
                                        URI.create(remoteWriteUrl),
                                        Map.of(
                                            "Content-Type", payload.contentType(),
                                            CONTENT_ENCODING_HEADER, SNAPPY_ENCODING,
                                            REMOTE_WRITE_VERSION_HEADER, payload.remoteWriteVersion()),
                                        payload.snappyBody(),
                                        AwsSigningServiceName.APS,
                                        awsRegion);
    }
}
