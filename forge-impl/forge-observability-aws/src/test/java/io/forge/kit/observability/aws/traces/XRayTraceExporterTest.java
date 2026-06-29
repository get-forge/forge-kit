package io.forge.kit.observability.aws.traces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.forge.kit.http.aws.AwsSignedHttpRequest;
import io.forge.kit.http.aws.AwsSignedHttpResponse;
import io.forge.kit.http.aws.AwsSigningServiceName;
import io.forge.kit.http.aws.SignedHttpTransport;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpMethod;

class XRayTraceExporterTest
{
    @Test
    void export_sendsOtlpProtobufToXRay()
    {
        final AtomicReference<AwsSignedHttpRequest> capturedRequest = new AtomicReference<>();
        final SignedHttpTransport transport = request ->
        {
            capturedRequest.set(request);
            return new AwsSignedHttpResponse(200, new byte[0]);
        };
        final XRayTraceExporter exporter = new XRayTraceExporter(
                                                                 transport,
                                                                 "https://xray.us-west-2.amazonaws.com/v1/traces",
                                                                 "us-west-2");
        final TestSpanData span = TestSpanData.builder()
            .setName("auth-login")
            .setKind(SpanKind.SERVER)
            .setStatus(StatusData.ok())
            .setStartEpochNanos(1_000L)
            .setEndEpochNanos(2_000L)
            .setHasEnded(true)
            .setResource(Resource.create(io.opentelemetry.api.common.Attributes.of(
                AttributeKey.stringKey("service.name"), "auth-service")))
            .build();

        final CompletableResultCode result = exporter.export(List.of(span));

        assertTrue(result.isSuccess());
        final AwsSignedHttpRequest request = capturedRequest.get();
        assertEquals(SdkHttpMethod.POST, request.method());
        assertEquals(AwsSigningServiceName.XRAY, request.signingService());
        assertEquals("application/x-protobuf", request.headers().get("Content-Type"));
        assertTrue(request.body().length > 0);
    }
}
