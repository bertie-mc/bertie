package io.github.bertie_mc.testing.client;

/** A scoped in-process dedicated server created by a client test. */
public interface DedicatedServerContext extends ServerContext, AutoCloseable {
    DedicatedServerConnection connect();

    @Override
    void close();
}
