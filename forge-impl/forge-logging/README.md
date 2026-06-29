# forge-logging

## Overview

`forge-logging` provides **structured logging for Quarkus HTTP services**:

- Opt-in method entry logging via `@LogMethodEntry`
- HTTP correlation ID propagation (MDC and headers)
- Sensitive data masking in log output

It is designed for production observability backends (for example, CloudWatch Logs) without prescribing a specific trace exporter.

---

## Key Features

- Opt-in method entry logging via `@LogMethodEntry` (CDI interceptor)
- HTTP correlation ID propagation via MDC and `X-Correlation-Id` headers, including outbound REST client forwarding
- Sensitive data masking in log output (secrets, tokens, and PII)
- JSON logging configuration examples for deployed profiles

---

## Design Principles

- Correlation identifiers are explicit and request-scoped
- Sensitive values are redacted before logs leave the process
- Trace identifiers come from optional OpenTelemetry MDC fields at the application level

---

## Typical Use Cases

- Annotated method entry logging on domain services or REST resources
- Cross-service request correlation via HTTP headers and MDC
- Structured JSON logs and compliance-friendly redaction in deployed environments

---

## Usage

Add `forge-logging-api` for `@LogMethodEntry` and `forge-logging` for runtime filters and the interceptor:

```xml
<dependency>
  <groupId>io.forge</groupId>
  <artifactId>forge-logging-api</artifactId>
  <version>${io.forge.version}</version>
</dependency>
<dependency>
  <groupId>io.forge</groupId>
  <artifactId>forge-logging</artifactId>
  <version>${io.forge.version}</version>
</dependency>
```

Copy the relevant properties from the example configuration file.

Correlation ID filters, the sensitive-data log filter, and the `@LogMethodEntry` interceptor are registered automatically via CDI.

### Method Entry Logging

Annotate methods with `@LogMethodEntry` from `forge-logging-api`:

```java
@LogMethodEntry
public Response getData() {
    // Logs: "DataResource#getData"
    return Response.ok().build();
}

@LogMethodEntry(message = "for user: %s", argPaths = {"#username"})
public Response login(LoginRequest request) {
    // Logs: "AuthService#login for user: {username}"
    return Response.ok().build();
}
```

The interceptor lives in this module because it produces log records that the sensitive-data filter must understand.

For local development, include MDC fields in `quarkus.log.console.format` (see the example file). JSON logging with `mdc.flat-fields=true` is used on deployed profiles.

Optional: add `quarkus-opentelemetry` at the application level if you want `traceId` and `spanId` in MDC.

---

## Examples

See: [examples/forge-logging](../../examples/forge-logging) for configuration and `@LogMethodEntry` examples.

Reference implementations:

- [`LogMethodEntryInterceptor`](src/main/java/io/forge/kit/logging/impl/LogMethodEntryInterceptor.java) — opt-in method entry logging
- [`CorrelationIdFilter`](src/main/java/io/forge/kit/logging/impl/CorrelationIdFilter.java) — inbound MDC and response header propagation
- [`CorrelationIdClientRequestFilter`](src/main/java/io/forge/kit/logging/impl/CorrelationIdClientRequestFilter.java) — outbound REST client header forwarding
- [`SensitiveDataLogFilter`](src/main/java/io/forge/kit/logging/impl/SensitiveDataLogFilter.java) — log output redaction filter

---
