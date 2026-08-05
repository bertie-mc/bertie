package io.github.bertie_mc.testing.client.context;

/**
 * A scoped in-process dedicated server created by a client test.
 *
 * <p>This context owns the server and is intended for a try-with-resources statement. Closing it
 * also closes any connections created through {@link #connect()}.
 */
public interface DedicatedServerContext extends ServerContext, AutoCloseable {
    /**
     * Connects the test client to this server.
     *
     * <p>The returned connection is independently closeable; closing it disconnects the client but
     * does not stop this server.
     *
     * @return an owned dedicated-server connection
     */
    DedicatedServerConnection connect();

    /** Stops the in-process dedicated server and closes its remaining connections. */
    @Override
    void close();
}
