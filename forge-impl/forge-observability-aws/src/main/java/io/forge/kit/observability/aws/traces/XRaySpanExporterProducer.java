package io.forge.kit.observability.aws.traces;

import io.forge.kit.http.aws.SignedHttpTransport;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Registers {@link XRayTraceExporter} with Quarkus OpenTelemetry when X-Ray export is enabled.
 */
@ApplicationScoped
public final class XRaySpanExporterProducer
{
    private static final SpanExporter DISABLED = new SpanExporter()
    {
        @Override
        public CompletableResultCode export(final Collection<SpanData> spans)
        {
            return CompletableResultCode.ofSuccess();
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
    };

    @Inject
    SignedHttpTransport transport;

    @ConfigProperty(name = "forge.observability.xray.export.enabled")
    boolean exportEnabled;

    @ConfigProperty(name = "forge.observability.xray.otlp.endpoint")
    String otlpEndpoint;

    @ConfigProperty(name = "aws.region")
    String awsRegion;

    /**
     * Supplies the X-Ray span exporter for {@code quarkus.otel.traces.exporter=cdi}.
     */
    @Produces
    @Singleton
    SpanExporter xraySpanExporter()
    {
        if (!exportEnabled)
        {
            return DISABLED;
        }

        return new XRayTraceExporter(transport, otlpEndpoint, awsRegion);
    }
}
