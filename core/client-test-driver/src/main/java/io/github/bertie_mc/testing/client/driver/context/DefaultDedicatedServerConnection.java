package io.github.bertie_mc.testing.client.driver.context;

import io.github.bertie_mc.testing.client.context.DedicatedServerConnection;
import io.github.bertie_mc.testing.client.driver.world.ClientWorldLoading;
import java.net.InetSocketAddress;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** Owns the client's connection to an in-process dedicated server. */
final class DefaultDedicatedServerConnection extends DefaultServerConnection
        implements DedicatedServerConnection {
    private final InetSocketAddress address;
    private boolean closed;

    private DefaultDedicatedServerConnection(
            DefaultClientTestContext context,
            DefaultDedicatedServerContext server,
            InetSocketAddress address) {
        super(context, server);
        this.address = address;
    }

    static DefaultDedicatedServerConnection connect(
            DefaultClientTestContext context,
            DefaultDedicatedServerContext server,
            InetSocketAddress address) {
        String addressText = formatAddress(address);
        ServerAddress parsed = ServerAddress.parseString(addressText);
        context.runOnClient(client -> {
            ServerData data = new ServerData(
                    "Bertie client-test server", addressText, ServerData.Type.OTHER);
            ConnectScreen.startConnecting(
                    client.screen,
                    client,
                    parsed,
                    data,
                    false,
                    null);
        });
        ClientWorldLoading.waitForWorld(
                context,
                "the dedicated-server connection",
                client -> client.level != null && client.player != null);
        return new DefaultDedicatedServerConnection(context, server, address);
    }

    @Override
    public InetSocketAddress address() {
        return address;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        context.disconnectToTitle();
    }

    private static String formatAddress(InetSocketAddress address) {
        String host = address.getHostString();
        if (host.indexOf(':') >= 0 && !host.startsWith("[")) {
            host = "[" + host + "]";
        }
        return host + ":" + address.getPort();
    }
}
