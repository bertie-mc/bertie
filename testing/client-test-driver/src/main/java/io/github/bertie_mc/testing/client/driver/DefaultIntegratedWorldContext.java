package io.github.bertie_mc.testing.client.driver;

import io.github.bertie_mc.testing.client.IntegratedWorldContext;
import io.github.bertie_mc.testing.client.ServerConnection;
import io.github.bertie_mc.testing.client.ServerContext;
import net.minecraft.client.server.IntegratedServer;

final class DefaultIntegratedWorldContext implements IntegratedWorldContext {
    private final DefaultClientTestContext context;
    private final DefaultServerContext server;
    private final DefaultServerConnection connection;
    private boolean closed;

    DefaultIntegratedWorldContext(DefaultClientTestContext context, IntegratedServer integratedServer) {
        this.context = context;
        this.server = new DefaultServerContext(context, integratedServer);
        this.connection = new DefaultServerConnection(context, server);
    }

    @Override
    public ServerContext server() {
        return server;
    }

    @Override
    public ServerConnection connection() {
        return connection;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        context.disconnectToTitle();
    }
}
