package io.forge.kit.observability.aws.encode;

import io.opentelemetry.exporter.internal.otlp.traces.TraceRequestMarshaler;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;

/**
 * Encodes OpenTelemetry spans into OTLP protobuf payloads for X-Ray ingestion.
 */
public final class OtlpTracePayloadEncoder
{
    private OtlpTracePayloadEncoder()
    {
    }

    /**
     * Serializes spans into an OTLP ExportTraceServiceRequest protobuf body.
     */
    public static byte[] encode(final Collection<SpanData> spans)
    {
        if (spans.isEmpty())
        {
            return new byte[0];
        }
        try
        {
            final TraceRequestMarshaler marshaler = TraceRequestMarshaler.create(spans);
            final ByteArrayOutputStream output = new ByteArrayOutputStream(marshaler.getBinarySerializedSize());
            marshaler.writeBinaryTo(output);

            return output.toByteArray();
        }
        catch (final IOException exception)
        {
            throw new UncheckedIOException("Failed to encode OTLP trace payload", exception);
        }
    }
}
