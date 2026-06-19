package io.forge.kit.throttle.impl.infrastructure;

import static org.hamcrest.Matchers.equalTo;

import io.forge.kit.throttle.api.infrastructure.RateLimiter;
import io.forge.kit.throttle.impl.test.ThrottlingEnabledTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for rate limiting functionality.
 * <p>
 * These tests verify that rate limiting works correctly for both authenticated and unauthenticated requests.
 * The tests use the {@code /test/secured} endpoint for authenticated scenarios, which is semantically
 * correct as it represents an endpoint that would require authentication in a production environment.
 * <p>
 * <b>Note:</b> The {@code @Secured} annotation and authentication implementation are not implemented in the forge-kit repo.
 * This means that while the tests call {@code /test/secured} (which is the correct semantic choice), the endpoint does not
 * enforce authentication. However, this does not affect the validity of these rate-limiting tests, as the rate limiting
 * filter extracts user identity from JWT headers independently of whether authentication is enforced by the endpoint.
 * The rate-limiting behavior is tested correctly regardless of the authentication enforcement status.
 */
@QuarkusTest
@TestProfile(ThrottlingEnabledTestProfile.class)
class RateLimitingIT
{
    @Inject
    RateLimiter rateLimiter;

    @BeforeEach
    @AfterEach
    void resetRateLimiterState()
    {
        // ApplicationScoped limiter persists across tests; reset via interface to work with CDI proxies
        rateLimiter.resetForTests();
    }

    @Test
    void anonymousRequestsAreRateLimited()
    {
        // Send requests up to the limit (10 per test config for unauthenticated)
        // Each request should succeed and consume one token from the bucket
        for (int index = 0; index < 10; index++)
        {
            RestAssured.given()
                .contentType("application/json")
                .body("""
                        {
                          "username": "anonymous@example.com",
                          "password": "whatever"
                        }
                    """)
                .when()
                .post("/test")
                .then()
                .statusCode(200);
        }

        // The 11th request should be rate limited (bucket is now empty)
        RestAssured.given()
            .contentType("application/json")
            .body("""
                    {
                      "username": "anonymous@example.com",
                      "password": "whatever"
                    }
                """)
            .when()
            .post("/test")
            .then()
            .statusCode(429)
            .header("X-RateLimit-Limit", equalTo("10"));
    }

    @Test
    void authenticatedRequestsAreRateLimited()
    {
        final String jwtToken = buildTestJwtToken(Map.of("username", "test-user@example.com"));

        // Send requests up to the limit (10 per test config for authenticated)
        // Each request should succeed and consume one token from the user's bucket
        for (int index = 0; index < 10; index++)
        {
            RestAssured.given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + jwtToken)
                .body("""
                        {
                          "username": "test-user@example.com",
                          "password": "whatever"
                        }
                    """)
                .when()
                .post("/test/secured")
                .then()
                .statusCode(200);
        }

        // The 11th request should be rate limited (user's bucket is now empty)
        RestAssured.given()
            .contentType("application/json")
            .header("Authorization", "Bearer " + jwtToken)
            .body("""
                    {
                      "username": "test-user@example.com",
                      "password": "whatever"
                    }
                """)
            .when()
            .post("/test/secured")
            .then()
            .statusCode(429)
            .header("X-RateLimit-Limit", equalTo("10"));
    }

    @Test
    void authenticatedAndUnauthenticatedRequestsUseSeparateBuckets()
    {
        final String jwtToken = buildTestJwtToken(Map.of("username", "user1@example.com"));

        // Consume all unauthenticated tokens (10 requests)
        for (int index = 0; index < 10; index++)
        {
            RestAssured.given()
                .contentType("application/json")
                .body("{}")
                .when()
                .post("/test")
                .then()
                .statusCode(200);
        }

        // Unauthenticated should now be rate limited
        RestAssured.given()
            .contentType("application/json")
            .body("{}")
            .when()
            .post("/test")
            .then()
            .statusCode(429);

        // But authenticated requests should still work (separate bucket)
        RestAssured.given()
            .contentType("application/json")
            .header("Authorization", "Bearer " + jwtToken)
            .body("{}")
            .when()
            .post("/test/secured")
            .then()
            .statusCode(200);
    }

    @Test
    void differentUsersHaveSeparateBuckets()
    {
        final String user1Token = buildTestJwtToken(Map.of("username", "user1@example.com"));
        final String user2Token = buildTestJwtToken(Map.of("username", "user2@example.com"));

        // User1 consumes all their tokens (10 requests)
        for (int index = 0; index < 10; index++)
        {
            RestAssured.given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + user1Token)
                .body("{}")
                .when()
                .post("/test/secured")
                .then()
                .statusCode(200);
        }

        // User1 should now be rate limited
        RestAssured.given()
            .contentType("application/json")
            .header("Authorization", "Bearer " + user1Token)
            .body("{}")
            .when()
            .post("/test/secured")
            .then()
            .statusCode(429);

        // But User2 should still work (separate bucket)
        RestAssured.given()
            .contentType("application/json")
            .header("Authorization", "Bearer " + user2Token)
            .body("{}")
            .when()
            .post("/test/secured")
            .then()
            .statusCode(200);
    }

    private String buildTestJwtToken(final Map<String, String> claims)
    {
        final String headerJson = "{\"alg\":\"none\"}";

        final StringBuilder payloadBuilder = new StringBuilder();
        payloadBuilder.append("{");

        boolean first = true;
        for (Map.Entry<String, String> entry : claims.entrySet())
        {
            if (!first)
            {
                payloadBuilder.append(",");
            }
            first = false;
            payloadBuilder.append("\"")
                .append(entry.getKey())
                .append("\":\"")
                .append(entry.getValue())
                .append("\"");
        }

        payloadBuilder.append("}");

        final String header = base64UrlEncode(headerJson);
        final String payload = base64UrlEncode(payloadBuilder.toString());

        return header + "." + payload + ".signature";
    }

    private String base64UrlEncode(final String value)
    {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
