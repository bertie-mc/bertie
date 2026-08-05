package io.github.bertie_mc.testing.client.context;

/**
 * A scoped client connection to an integrated server.
 *
 * <p>This context owns the running integrated world and is intended for a try-with-resources
 * statement.
 */
public interface IntegratedWorldContext extends AutoCloseable {
    /**
     * Returns operations for the integrated server.
     *
     * @return the integrated-server context
     */
    ServerContext server();

    /**
     * Returns the connection joining the client and integrated server.
     *
     * @return the integrated-server connection
     */
    ServerConnection connection();

    /** Closes the integrated world and returns the client to the title screen. */
    @Override
    void close();
}
