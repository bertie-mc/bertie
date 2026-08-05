package io.github.bertie_mc.testing.client.driver.context;

import io.github.bertie_mc.testing.client.context.DedicatedServerConnection;
import io.github.bertie_mc.testing.client.context.DedicatedServerContext;
import io.github.bertie_mc.testing.client.driver.ClientTestResources;
import io.github.bertie_mc.testing.client.driver.server.InProcessDedicatedServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/** Adapts an in-process dedicated-server launch to the client-test context API. */
public final class DefaultDedicatedServerContext extends DefaultServerContext
        implements DedicatedServerContext {
    private final InProcessDedicatedServer.Launch launch;
    private final ClientTestResources connections = new ClientTestResources();
    private boolean closed;

    public DefaultDedicatedServerContext(
            DefaultClientTestContext context, InProcessDedicatedServer.Launch launch) {
        super(context, launch.server());
        this.launch = launch;
    }

    @Override
    public DedicatedServerConnection connect() {
        if (closed) {
            throw new IllegalStateException("The dedicated server context is closed");
        }
        InetSocketAddress address = connectableAddress(launch.boundAddress());
        return connections.own(DefaultDedicatedServerConnection.connect(
                context, this, address));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        Throwable failure = null;
        try {
            connections.close();
        } catch (Throwable closeFailure) {
            failure = closeFailure;
        }
        try {
            launch.abort();
            context.awaitInfrastructure(
                    "the dedicated server threads to terminate", launch.stopped());
        } catch (Throwable closeFailure) {
            failure = ClientTestResources.append(failure, closeFailure);
        }
        ClientTestResources.rethrow(failure);
    }

    private static InetSocketAddress connectableAddress(InetSocketAddress bound) {
        InetAddress address = bound.getAddress();
        if (address != null && address.isAnyLocalAddress()) {
            return new InetSocketAddress(InetAddress.getLoopbackAddress(), bound.getPort());
        }
        return bound;
    }
}
