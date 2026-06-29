# forge-common

## Overview

`forge-common` contains **shared utilities and primitives** used across Forge Kit modules.

It focuses on consistency, correctness, and developer ergonomics rather than feature richness.

---

## Key Features

- Validation and exception mapping utilities
- REST-layer helpers
- Test utilities for deterministic infrastructure testing
- Clean Architecture–aligned structure

---

## Design Principles

- Prefer small, explicit utilities over large abstractions
- Make cross-cutting concerns visible
- Avoid hidden behaviour and implicit coupling

---

## Typical Use Cases

- Standardised error handling
- Validation and constraint mapping
- Shared REST conventions
- Test infrastructure support

---

## Usage

`forge-common` provides utilities that are automatically wired in when included as a dependency. No additional configuration is required.

### Validation Exception Mapper

The `ValidationExceptionMapper` automatically converts `ConstraintViolationException` to 400 Bad
Request responses. Simply use Jakarta Bean Validation annotations:

```java
@POST
@Path("/users")
public Response createUser(@Valid CreateUserRequest request) {
    // If validation fails, a 400 response is automatically returned
    return Response.ok().build();
}

record CreateUserRequest(
    @NotBlank String name,
    @Email String email
) {}
```

---

## Examples

See: [examples/forge-common](../../examples/forge-common)

Code examples demonstrate:
- Using Jakarta Bean Validation with automatic exception mapping

---
