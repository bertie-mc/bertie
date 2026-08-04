package io.github.bertie_mc.testing.client;

import java.net.InetSocketAddress;

/** A scoped client connection to an in-process dedicated server. */
public interface DedicatedServerConnection extends ServerConnection, AutoCloseable {
    InetSocketAddress address();

    @Override
    void close();
}
