package io.github.bertie_mc.testing.client.context;

import java.net.InetSocketAddress;

/**
 * A scoped client connection to an in-process dedicated server.
 *
 * <p>This connection is intended for a try-with-resources statement.
 */
public interface DedicatedServerConnection extends ServerConnection, AutoCloseable {
    /**
     * Returns the server address used by the client.
     *
     * @return the dedicated server's connectable address
     */
    InetSocketAddress address();

    /** Disconnects the client and returns it to the title screen without stopping the server. */
    @Override
    void close();
}
