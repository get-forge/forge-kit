package io.forge.kit.http.aws;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

/**
 * Converts AWS SDK credentials into signing identities for SigV4.
 */
final class AwsCredentialsIdentityConverter
{
    private AwsCredentialsIdentityConverter()
    {
    }

    static AwsCredentialsIdentity toIdentity(final AwsCredentials credentials)
    {
        if (credentials instanceof AwsSessionCredentials sessionCredentials)
        {
            return AwsSessionCredentialsIdentity.create(
                sessionCredentials.accessKeyId(),
                sessionCredentials.secretAccessKey(),
                sessionCredentials.sessionToken());
        }
        return AwsCredentialsIdentity.create(credentials.accessKeyId(), credentials.secretAccessKey());
    }
}
