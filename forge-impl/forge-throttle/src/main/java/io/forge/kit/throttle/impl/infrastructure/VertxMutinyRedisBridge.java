package io.forge.kit.throttle.impl.infrastructure;

import io.vertx.core.Future;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisConnection;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Adapts Quarkus Mutiny Redis client to the Vert.x Redis interface required by Bucket4j.
 */
final class VertxMutinyRedisBridge implements Redis
{
    private final io.vertx.mutiny.redis.client.Redis mutinyRedis;

    VertxMutinyRedisBridge(final io.vertx.mutiny.redis.client.Redis mutinyRedis)
    {
        this.mutinyRedis = Objects.requireNonNull(mutinyRedis, "mutinyRedis");
    }

    @Override
    public Future<RedisConnection> connect()
    {
        throw new UnsupportedOperationException("Connection management is handled by Quarkus Redis client");
    }

    @Override
    public void close()
    {
        mutinyRedis.close();
    }

    @Override
    public Future<Response> send(final Request request)
    {
        return Future.fromCompletionStage(
            mutinyRedis.send(new io.vertx.mutiny.redis.client.Request(request))
                .subscribe()
                .asCompletionStage()
                .thenApply(VertxMutinyRedisBridge::toDelegate)
        );
    }

    @Override
    public Future<List<Response>> batch(final List<Request> requests)
    {
        final List<io.vertx.mutiny.redis.client.Request> mutinyRequests = requests.stream()
            .map(io.vertx.mutiny.redis.client.Request::new)
            .collect(Collectors.toList());

        return Future.fromCompletionStage(
            mutinyRedis.batch(mutinyRequests)
                .subscribe()
                .asCompletionStage()
                .thenApply(responses -> responses.stream()
                    .map(VertxMutinyRedisBridge::toDelegate)
                    .collect(Collectors.toList()))
        );
    }

    private static Response toDelegate(final io.vertx.mutiny.redis.client.Response response)
    {
        // Redis NIL / missing keys can surface as a null Mutiny response; Bucket4j expects null delegate.
        return response == null ? null : response.getDelegate();
    }
}
