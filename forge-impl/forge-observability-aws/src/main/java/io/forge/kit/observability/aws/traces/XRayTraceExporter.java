package io.forge.kit.observability.aws.traces;

import io.forge.kit.http.aws.AwsSignedHttpRequest;
import io.forge.kit.http.aws.AwsSignedHttpResponse;
import io.forge.kit.http.aws.AwsSigningServiceName;
import io.forge.kit.http.aws.SignedHttpTransport;
import io.forge.kit.observability.aws.encode.OtlpTracePayloadEncoder;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import org.jboss.logging.Logger;
import software.amazon.awssdk.http.SdkHttpMethod;

/**
 * Exports OpenTelemetry spans to the AWS X-Ray OTLP endpoint with SigV4 signing.
 * <p>
 * Not a CDI bean: created by {@link XRaySpanExporterProducer} at startup so OTel worker
 * threads never trigger lazy {@code @ConfigProperty} injection.
 */
public final class XRayTraceExporter implements SpanExporter
{
    private static final Logger LOGGER = Logger.getLogger(XRayTraceExporter.class);
    private static final String OTLP_PROTOBUF_CONTENT_TYPE = "application/x-protobuf";

    private final SignedHttpTransport transport;
    private final String otlpEndpoint;
    private final String awsRegion;

    XRayTraceExporter(final SignedHttpTransport transport, final String otlpEndpoint, final String awsRegion)
    {
        this.transport = transport;
        this.otlpEndpoint = otlpEndpoint;
        this.awsRegion = awsRegion;
    }

    @Override
    public CompletableResultCode export(final Collection<SpanData> spans)
    {
        if (spans.isEmpty())
        {
            return CompletableResultCode.ofSuccess();
        }
        try
        {
            final byte[] body = OtlpTracePayloadEncoder.encode(spans);
            final AwsSignedHttpResponse response = transport.send(buildRequest(body));
            return response.isSuccessful() ? CompletableResultCode.ofSuccess() : CompletableResultCode.ofFailure();
        }
        catch (final RuntimeException exception)
        {
            LOGGER.error("X-Ray OTLP export failed", exception);
            return CompletableResultCode.ofFailure();
        }
    }

    @Override
    public CompletableResultCode flush()
    {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown()
    {
        return CompletableResultCode.ofSuccess();
    }

    private AwsSignedHttpRequest buildRequest(final byte[] body)
    {
        return new AwsSignedHttpRequest(
                                        SdkHttpMethod.POST,
                                        URI.create(otlpEndpoint),
                                        Map.of("Content-Type", OTLP_PROTOBUF_CONTENT_TYPE),
                                        body,
                                        AwsSigningServiceName.XRAY,
                                        awsRegion);
    }
}
