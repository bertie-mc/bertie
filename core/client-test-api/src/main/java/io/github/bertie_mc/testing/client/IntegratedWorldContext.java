package io.github.bertie_mc.testing.client;

/** A scoped integrated world created by a client test. */
public interface IntegratedWorldContext extends AutoCloseable {
    ServerContext server();

    ServerConnection connection();

    @Override
    void close();
}
