package io.forge.kit.observability.api.encode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.forge.kit.observability.api.remotewrite.Label;
import io.forge.kit.observability.api.remotewrite.TimeSeries;
import io.forge.kit.observability.api.remotewrite.WriteRequest;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xerial.snappy.Snappy;

class PrometheusRemoteWriteEncoderTest
{
    private static final String NAME_LABEL = "__name__";

    private PrometheusMeterRegistry registry;
    private PrometheusRemoteWriteEncoder encoder;

    @BeforeEach
    void setUp()
    {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        encoder = new PrometheusRemoteWriteEncoder();
    }

    @Test
    void encode_IncludesCounterGaugeTimerAndSummarySeries()
    {
        registry.counter("requests_total", "service", "auth").increment(3);
        registry.gauge("heap_used_bytes", 42.0);
        registry.timer("http_latency").record(120, TimeUnit.MILLISECONDS);
        registry.summary("payload_size").record(512);

        final WriteRequest request = encodeCurrentSnapshots();

        assertSeriesPresent(request, "requests_total", "service", "auth", 3.0);
        assertSeriesPresent(request, "heap_used_bytes", 42.0);
        assertTrue(
            hasSeriesWithNamePrefix(request, "http_latency"),
            "Timer should produce latency series");
        assertTrue(
            hasSeriesWithNamePrefix(request, "payload_size"),
            "Summary should produce sum and count series");
    }

    @Test
    void encode_ExpandsTimerHistogramBucketsWithLeLabel()
    {
        Timer.builder("api_duration")
            .tag("route", "/health")
            .publishPercentileHistogram()
            .register(registry)
            .record(50, TimeUnit.MILLISECONDS);

        final WriteRequest request = encodeCurrentSnapshots();
        final Optional<TimeSeries> bucketSeries = findBucketSeries(request, "api_duration");

        assertTrue(bucketSeries.isPresent(), "Expected _bucket series for percentile histogram timer");
        assertTrue(readLabel(bucketSeries.get(), "le").isPresent());
        assertEquals(Optional.of("/health"), readLabel(bucketSeries.get(), "route"));
        assertEquals(1, bucketSeries.get().getSamplesCount());
    }

    @Test
    void encodeSnappy_ProducesDecompressibleProtobufBody() throws IOException
    {
        registry.counter("events_total").increment();

        final RemoteWritePayload payload = encoder.encodeSnappy(currentSnapshots());
        final byte[] protobufBody = Snappy.uncompress(payload.snappyBody());
        final WriteRequest roundTrip = WriteRequest.parseFrom(protobufBody);

        assertEquals(RemoteWritePayload.DEFAULT_CONTENT_TYPE, payload.contentType());
        assertEquals(RemoteWritePayload.DEFAULT_REMOTE_WRITE_VERSION, payload.remoteWriteVersion());
        assertFalse(roundTrip.getTimeseriesList().isEmpty());
        assertSeriesPresent(roundTrip, "events_total", 1.0);
    }

    private WriteRequest encodeCurrentSnapshots()
    {
        return encoder.encode(currentSnapshots());
    }

    private MetricSnapshots currentSnapshots()
    {
        return registry.getPrometheusRegistry().scrape();
    }

    private void assertSeriesPresent(
                                     final WriteRequest request, final String metricName, final double expectedValue)
    {
        final Optional<TimeSeries> series = findSeriesByName(request, metricName);
        assertTrue(series.isPresent(), "Missing series: " + metricName);
        assertEquals(expectedValue, series.get().getSamples(0).getValue(), 0.0001);
    }

    private void assertSeriesPresent(
                                     final WriteRequest request, final String metricName, final String labelName, final String expectedLabelValue, final double expectedValue)
    {
        final Optional<TimeSeries> series = request.getTimeseriesList().stream()
            .filter(candidate -> metricName.equals(readLabel(candidate, NAME_LABEL).orElse(null)))
            .filter(candidate -> expectedLabelValue.equals(readLabel(candidate, labelName).orElse(null)))
            .findFirst();
        assertTrue(
            series.isPresent(),
            "Missing series: " + metricName + "{" + labelName + "=" + expectedLabelValue + "}");
        assertEquals(expectedValue, series.get().getSamples(0).getValue(), 0.0001);
    }

    private boolean hasSeriesWithNamePrefix(final WriteRequest request, final String namePrefix)
    {
        return request.getTimeseriesList().stream()
            .map(series -> readLabel(series, NAME_LABEL))
            .flatMap(Optional::stream)
            .anyMatch(metricName -> metricName.startsWith(namePrefix));
    }

    private Optional<TimeSeries> findBucketSeries(final WriteRequest request, final String namePrefix)
    {
        return request.getTimeseriesList().stream()
            .filter(series -> readLabel(series, NAME_LABEL)
                .map(metricName -> metricName.startsWith(namePrefix) && metricName.endsWith("_bucket"))
                .orElse(false))
            .findFirst();
    }

    private Optional<TimeSeries> findSeriesByName(final WriteRequest request, final String metricName)
    {
        return request.getTimeseriesList().stream()
            .filter(series -> metricName.equals(readLabel(series, NAME_LABEL).orElse(null)))
            .findFirst();
    }

    private Optional<String> readLabel(final TimeSeries series, final String labelName)
    {
        return series.getLabelsList().stream()
            .filter(label -> labelName.equals(label.getName()))
            .map(Label::getValue)
            .findFirst();
    }
}
