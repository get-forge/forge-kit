package io.forge.kit.examples.logging;

import io.forge.kit.logging.api.LogMethodEntry;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Example demonstrating the use of {@code @LogMethodEntry} annotation
 * for automatic method entry logging.
 * 
 * <p>The {@code LogMethodEntryInterceptor} is automatically wired in
 * when forge-logging is included as a dependency. You only need to
 * annotate methods or classes with {@code @LogMethodEntry}.
 * 
 * <p>This example shows:
 * <ul>
 *   <li>Basic method-level logging</li>
 *   <li>Class-level logging (applies to all methods)</li>
 *   <li>Logging with custom message format</li>
 *   <li>Logging with parameter extraction</li>
 * </ul>
 */
@Path("/api/examples")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LogMethodEntryExample
{
    /**
     * Basic method-level logging.
     * 
     * <p>This will log: {@code LogMethodEntryExample#getPublicData}
     */
    @GET
    @Path("/public")
    @LogMethodEntry
    public Response getPublicData()
    {
        return Response.ok("Public data").build();
    }

    /**
     * Logging with custom message format.
     * 
     * <p>This will log: {@code LogMethodEntryExample#getUserData for user: {userId}}
     * 
     * <p>The first parameter is automatically used for the format string.
     */
    @GET
    @Path("/user/{userId}")
    @LogMethodEntry(message = "for user: %s")
    public Response getUserData(@PathParam("userId") String userId)
    {
        return Response.ok("User data for: " + userId).build();
    }

    /**
     * Logging with explicit parameter path.
     * 
     * <p>This will log: {@code LogMethodEntryExample#createUser for email: {email}}
     * 
     * <p>The {@code argPaths} parameter specifies which parameter to extract.
     * The {@code "#"} prefix means "first parameter", and {@code "emailAddress"}
     * is the property to extract from the request object.
     */
    @GET
    @Path("/create")
    @LogMethodEntry(message = "for email: %s", argPaths = {"#emailAddress"})
    public Response createUser(CreateUserRequest request)
    {
        return Response.ok("User created").build();
    }

    /**
     * Class-level logging applies to all methods in the class.
     * 
     * <p>All methods in this inner class will log their entry.
     */
    @LogMethodEntry
    public static class AdminResource
    {
        /**
         * This will log: {@code AdminResource#getUsers}
         */
        @GET
        @Path("/admin/users")
        public Response getUsers()
        {
            return Response.ok("Users list").build();
        }

        /**
         * This will log: {@code AdminResource#getStats}
         */
        @GET
        @Path("/admin/stats")
        public Response getStats()
        {
            return Response.ok("Statistics").build();
        }
    }
}

/**
 * Example request DTO.
 */
record CreateUserRequest(String emailAddress, String name)
{
}
