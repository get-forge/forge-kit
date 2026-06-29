package io.forge.kit.observability.api.encode;

import java.util.Arrays;

/**
 * Snappy-compressed Prometheus remote-write body and protocol headers.
 */
public record RemoteWritePayload(byte[] snappyBody, String contentType, String remoteWriteVersion)
{

    public static final String DEFAULT_CONTENT_TYPE = "application/x-protobuf";
    public static final String DEFAULT_REMOTE_WRITE_VERSION = "0.1.0";

    public RemoteWritePayload
    {
        snappyBody = Arrays.copyOf(snappyBody, snappyBody.length);
    }

    @Override
    public byte[] snappyBody()
    {
        return Arrays.copyOf(snappyBody, snappyBody.length);
    }
}
