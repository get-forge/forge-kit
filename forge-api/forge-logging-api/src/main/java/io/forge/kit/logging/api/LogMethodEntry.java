package io.forge.kit.logging.api;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for automatic entry logging.
 * Include {@code forge-logging} on the runtime classpath for the interceptor to run.
 * Logs use the format: {@code ClassName#methodName}.
 *
 * <p>This annotation can be used on methods or classes. When used on a class,
 * it applies to all public methods in that class.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @LogMethodEntry
 * public Optional<ActorResponse> getProfile(final String actorId) {
 * // Method body
 * }
 * }
 * </pre>
 * This will log: {@code ActorService#getProfile}
 *
 * <p>Optional message format:
 * <pre>
 * {@code
 * @LogMethodEntry(message = "for actor: %s", args = {0})
 * public Optional<ActorResponse> getProfile(final String actorId) {
 * // Method body
 * }
 * }
 * </pre>
 * This will log: {@code ActorService#getProfile for actor: {actorId}}
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface LogMethodEntry
{
    /**
     * Optional message format string to append after the method name.
     * Uses the same format as {@link java.util.Formatter}.
     *
     * <p>Example: {@code "for actor: %s"} will produce:
     * {@code ActorService#getProfile for actor: {argValue}}
     *
     * <p>If {@link #argPaths()} is empty and the message contains format specifiers,
     * the first parameter (index 0) will be used automatically for single-parameter methods.
     *
     * <p>Marked as {@link Nonbinding} so that different message values are treated
     * as the same interceptor binding. The interceptor reads the actual message
     * from the annotation at runtime.
     *
     * @return the message format string, or empty string if not specified
     */
    @Nonbinding
    String message() default "";

    /**
     * Property paths to extract from method arguments for the message format.
     * Supports nested property access using reflection.
     *
     * <p>If empty and {@link #message()} contains format specifiers, the first parameter
     * (index 0) will be used automatically.
     *
     * <p>Path format: {@code "parameter#property#nestedProperty"} or {@code "#property#nestedProperty"}
     * <ul>
     * <li>If path starts with {@code "#"}, defaults to first parameter (index 0): {@code "#username"}</li>
     * <li>Parameter must be an index (0-based): {@code "0#username"} or {@code "1#limit"}</li>
     * <li>Properties follow JavaBean convention: {@code "username"} calls {@code getUsername()}</li>
     * <li>Properties can be nested: {@code "#registerRequest#emailAddress"}</li>
     * </ul>
     *
     * <p>Examples:
     * <ul>
     * <li>{@code @LogMethodEntry(message = "for actor: %s")} - auto-uses first parameter</li>
     * <li>{@code @LogMethodEntry(message = "for user: %s", argPaths = {"#username"})} - shorthand for first parameter</li>
     * <li>{@code @LogMethodEntry(message = "for actor: %s", argPaths = {"#registerRequest#emailAddress"})}</li>
     * <li>{@code @LogMethodEntry(message = "actor: %s, limit: %d", argPaths = {"#actorId", "1"})} - first and second parameters</li>
     * </ul>
     *
     * <p>Marked as {@link Nonbinding} so that different argPaths values are treated
     * as the same interceptor binding. The interceptor reads the actual argPaths
     * from the annotation at runtime.
     *
     * @return array of property paths to use for message formatting
     */
    @Nonbinding
    String[] argPaths() default {};
}
