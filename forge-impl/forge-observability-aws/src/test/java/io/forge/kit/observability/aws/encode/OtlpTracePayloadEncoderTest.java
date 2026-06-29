package io.forge.kit.observability.aws.encode;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.List;
import org.junit.jupiter.api.Test;

class OtlpTracePayloadEncoderTest
{
    @Test
    void encode_ProducesNonEmptyProtobufForSpanBatch()
    {
        final TestSpanData span = TestSpanData.builder()
            .setName("auth-login")
            .setKind(SpanKind.SERVER)
            .setStatus(StatusData.ok())
            .setStartEpochNanos(1_000L)
            .setEndEpochNanos(2_000L)
            .setHasEnded(true)
            .setResource(Resource.create(
                io.opentelemetry.api.common.Attributes.of(AttributeKey.stringKey("service.name"), "auth-service")))
            .build();

        final byte[] payload = OtlpTracePayloadEncoder.encode(List.of(span));

        assertTrue(payload.length > 0);
    }
}
