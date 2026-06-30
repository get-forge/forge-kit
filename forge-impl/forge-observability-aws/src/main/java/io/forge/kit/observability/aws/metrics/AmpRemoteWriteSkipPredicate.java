package io.forge.kit.observability.aws.metrics;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Skips AMP push jobs when remote write is disabled in runtime config.
 */
@ApplicationScoped
public final class AmpRemoteWriteSkipPredicate implements Scheduled.SkipPredicate
{
    @ConfigProperty(name = "forge.observability.amp.remote-write.enabled")
    boolean remoteWriteEnabled;

    @Override
    public boolean test(final ScheduledExecution execution)
    {
        return !remoteWriteEnabled;
    }
}
