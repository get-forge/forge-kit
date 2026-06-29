# forge-observability-aws

## Overview

`forge-observability-aws` provides **AWS-native observability exporters** for Quarkus services:

- **Metrics** — scheduled push of Micrometer Prometheus metrics to Amazon Managed Prometheus (AMP) via remote write
- **Traces** — OTLP protobuf export to AWS X-Ray with SigV4 signing

Encoding lives in `forge-observability-api`; signing and HTTP delivery use `forge-http-aws`. No OpenTelemetry Collector sidecar is required.

Exporters are **profile-gated** with `@LookupIfProperty` so local and integration environments stay no-op unless explicitly enabled.

---

## Key Features

- PRW 1.0 encoding from Micrometer `MetricSnapshots` (Prometheus client v1.x / `quarkus-micrometer-registry-prometheus-v1`)
- Snappy-compressed remote write payloads for AMP (`aps` SigV4 service)
- OTLP trace export to X-Ray (`xray` SigV4 service) via OpenTelemetry SDK marshalers
- Quarkus scheduler integration for metrics push intervals
- CDI beans registered only when configuration enables each exporter

---

## Design Principles

- Glue, not protocols — reuse Micrometer and OpenTelemetry; do not fork exporters
- AWS concerns stay in kit modules; application repos own environment-specific URLs and profile gating
- Exporters fail loudly in logs without breaking the service request path

---

## Typical Use Cases

- Production metrics in Amazon Managed Grafana via AMP
- Distributed traces in AWS X-Ray from Quarkus OpenTelemetry
- Test/prod profile enablement while dev/int remain disabled

---

## Usage

Add both the API and AWS implementation modules (plus `forge-http-aws` transitively):

```xml
<dependency>
  <groupId>io.forge</groupId>
  <artifactId>forge-observability-api</artifactId>
  <version>${io.forge.version}</version>
</dependency>
<dependency>
  <groupId>io.forge</groupId>
  <artifactId>forge-observability-aws</artifactId>
  <version>${io.forge.version}</version>
</dependency>
```

Ensure the application also includes:

- `quarkus-micrometer-registry-prometheus-v1` — Prometheus v1 registry for `/q/metrics` and snapshot scrape
- `quarkus-opentelemetry` — trace SDK for X-Ray export (when traces are enabled)
- `quarkus-scheduler` — metrics push scheduling

### Amazon Managed Prometheus (metrics)

```properties
forge.observability.amp.remote-write.enabled=true
forge.observability.amp.remote-write.url=${AMP_REMOTE_WRITE_URL}
forge.observability.amp.push-interval=60s
aws.region=${AWS_REGION}
```

The task role (or equivalent credentials) needs `aps:RemoteWrite` on the AMP workspace.

### AWS X-Ray (traces)

```properties
forge.observability.xray.export.enabled=true
forge.observability.xray.otlp.endpoint=${XRAY_OTLP_ENDPOINT}
aws.region=${AWS_REGION}
```

OpenTelemetry exporter settings in the application should target the in-process pipeline; this module posts OTLP protobuf to the X-Ray endpoint with SigV4.

### Profile gating

Use Quarkus profile overrides so only deployed environments activate exporters:

```properties
%dev.forge.observability.amp.remote-write.enabled=false
%int.forge.observability.amp.remote-write.enabled=false
%test.forge.observability.amp.remote-write.enabled=true
%prod.forge.observability.amp.remote-write.enabled=true
```

Apply the same pattern for `forge.observability.xray.export.enabled`.

---

## Module split

| Module                    | Responsibility                                                                      |
|---------------------------|-------------------------------------------------------------------------------------|
| `forge-observability-api` | PRW 1.0 `WriteRequest` encoding, Snappy compression, `PrometheusRemoteWriteEncoder` |
| `forge-http-aws`          | SigV4-signed HTTP transport                                                         |
| `forge-observability-aws` | `AmpMetricsExporter`, `XRayTraceExporter`, OTLP payload encoder                     |

---

## Examples

Reference implementations:

- [`AmpMetricsExporter`](src/main/java/io/forge/kit/observability/aws/metrics/AmpMetricsExporter.java) — scheduled AMP remote write
- [`XRayTraceExporter`](src/main/java/io/forge/kit/observability/aws/traces/XRayTraceExporter.java) — X-Ray OTLP export
- [`PrometheusRemoteWriteEncoder`](../../forge-api/forge-observability-api/src/main/java/io/forge/kit/observability/api/encode/PrometheusRemoteWriteEncoder.java) — PRW encoding (API module)

---
