# forge-http-aws

## Overview

`forge-http-aws` provides **SigV4-signed outbound HTTP transport** for AWS endpoints that require request signing.

It is a thin wrapper around the AWS SDK URL-connection client and `AwsV4HttpSigner`, with retries and credential resolution via the default provider chain.

This module is AWS-specific. Use it when an endpoint expects SigV4 (for example, AMP remote write or X-Ray OTLP ingestion) rather than a generic bearer token or API key.

---

## Key Features

- `SignedHttpTransport` abstraction with an `AwsSignedHttpTransport` implementation
- Configurable AWS signing service name (`aps`, `xray`, and other SigV4 services)
- Automatic credential resolution (instance role, environment, profile)
- Bounded retries with exponential backoff on transient failures

---

## Design Principles

- Signing logic stays separate from payload encoding and scheduling
- Transport is stateless and injectable — exporters compose it rather than reimplement signing
- Failures surface as structured responses for observability and logging

---

## Typical Use Cases

- Amazon Managed Prometheus remote write (`aps` service)
- AWS X-Ray OTLP ingestion (`xray` service)
- Any custom integration that needs SigV4-signed POST/PUT to an AWS HTTP API

---

## Usage

Add the dependency:

```xml
<dependency>
  <groupId>io.forge</groupId>
  <artifactId>forge-http-aws</artifactId>
  <version>${io.forge.version}</version>
</dependency>
```

Inject `SignedHttpTransport` (or `AwsSignedHttpTransport`) and build a signed request:

```java
@Inject
SignedHttpTransport transport;

AwsSignedHttpResponse response = transport.send(
    new AwsSignedHttpRequest(
        SdkHttpMethod.POST,
        URI.create(remoteWriteUrl),
        Map.of(
            "Content-Type", "application/x-protobuf",
            "Content-Encoding", "snappy"),
        payloadBytes,
        awsRegion,
        AwsSigningServiceName.APS));
```

`forge-observability-aws` uses this transport for AMP and X-Ray exporters. Applications typically depend on that module rather than calling the transport directly.

---

## Examples

Reference implementations:

- [`AwsSignedHttpTransport`](src/main/java/io/forge/kit/http/aws/AwsSignedHttpTransport.java) — SigV4 signing and HTTP execution
- [`SignedHttpTransport`](src/main/java/io/forge/kit/http/aws/SignedHttpTransport.java) — transport contract consumed by exporters

---
