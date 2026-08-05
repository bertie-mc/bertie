package io.github.bertie_mc.testing.client.driver.server;

import io.github.bertie_mc.testing.client.driver.world.PreparedDedicatedWorld;
import io.github.bertie_mc.testing.client.driver.mixin.server.ServerConnectionListenerAccessor;
import io.netty.channel.ChannelFuture;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.server.Main;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;

/** Starts and observes one dedicated server inside the client JVM. */
public final class InProcessDedicatedServer {
    private static final AtomicReference<Launch> ACTIVE = new AtomicReference<>();
    private static final Properties DEFAULT_PROPERTIES = Util.make(new Properties(), properties -> {
        properties.setProperty("online-mode", "false");
        properties.setProperty("server-ip", "::1");
        properties.setProperty("server-port", "0");
        properties.setProperty("sync-chunk-writes", String.valueOf(Util.getPlatform() == Util.OS.WINDOWS));
        properties.setProperty("spawn-protection", "0");
        properties.setProperty("max-players", "1");
        properties.setProperty("max-tick-time", "-1");
    });

    private InProcessDedicatedServer() {}

    public static Launch begin(PreparedDedicatedWorld world, Properties customProperties) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(customProperties);

        Properties properties = new Properties();
        properties.putAll(DEFAULT_PROPERTIES);
        properties.putAll(customProperties);
        properties.setProperty("level-name", world.levelName());
        writeProperties(properties);

        Launch launch = new Launch();
        if (!ACTIVE.compareAndSet(null, launch)) {
            throw new IllegalStateException("An in-process dedicated server is already active");
        }

        Thread.ofPlatform().name("bertie-client-test-server-bootstrap").start(() -> {
            try {
                Main.main(new String[] {
                    "--nogui",
                    "--universe",
                    world.universe().toString()
                });
                launch.bootstrapReturned();
            } catch (Throwable failure) {
                launch.fail(failure);
            } finally {
                launch.bootstrapTerminated();
            }
        });
        return launch;
    }

    public static boolean ownsCurrentServerLifecycle() {
        return ACTIVE.get() != null;
    }

    public static void onServerThreadCreated(MinecraftServer server) {
        Launch launch = ACTIVE.get();
        if (launch != null && server instanceof DedicatedServer dedicatedServer) {
            launch.capture(dedicatedServer);
        }
    }

    public static void onServerThreadTerminated(MinecraftServer server) {
        Launch launch = ACTIVE.get();
        if (launch == null || !(server instanceof DedicatedServer dedicatedServer)) {
            return;
        }
        if (launch.server.get() == null) {
            launch.capture(dedicatedServer);
        }
        if (launch.server.get() == server) {
            launch.serverTerminated();
        }
    }

    /** Called from {@code DedicatedServer.initServer} once its TCP listener is ready. */
    public static void onServerReadyToLoad(DedicatedServer server) {
        Launch launch = ACTIVE.get();
        if (launch == null) {
            return;
        }
        launch.capture(server);
        if (launch.server.get() == server) {
            launch.started.complete(server);
        }
    }

    public static void onServerCrash(MinecraftServer server, CrashReport report) {
        Launch launch = ACTIVE.get();
        if (launch == null || !(server instanceof DedicatedServer dedicatedServer)) {
            return;
        }
        if (launch.server.get() == null) {
            launch.capture(dedicatedServer);
        }
        if (server == launch.server.get()) {
            launch.fail(report.getException());
        }
    }

    private static void writeProperties(Properties properties) {
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of("server.properties"))) {
            properties.store(writer, "Bertie in-process client-test server");
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write client-test server.properties", exception);
        }
    }

    /** Lifecycle and address information for one in-process dedicated-server launch. */
    public static final class Launch {
        private final AtomicReference<DedicatedServer> server = new AtomicReference<>();
        private final AtomicBoolean abortRequested = new AtomicBoolean();
        private final CompletableFuture<DedicatedServer> started = new CompletableFuture<>();
        private final CompletableFuture<Void> bootstrapTermination = new CompletableFuture<>();
        private final CompletableFuture<Void> serverTermination = new CompletableFuture<>();
        private final CompletableFuture<Void> stopped = CompletableFuture
                .allOf(bootstrapTermination, serverTermination)
                .whenComplete((ignored, failure) -> ACTIVE.compareAndSet(this, null));

        private Launch() {}

        public CompletableFuture<DedicatedServer> started() {
            return started;
        }

        public CompletableFuture<Void> stopped() {
            return stopped;
        }

        public InetSocketAddress boundAddress() {
            DedicatedServer dedicatedServer = server();
            var channels = ((ServerConnectionListenerAccessor) dedicatedServer.getConnection())
                    .bertie$getChannels();
            synchronized (channels) {
                for (ChannelFuture channel : channels) {
                    SocketAddress address = channel.channel().localAddress();
                    if (address instanceof InetSocketAddress inetAddress) {
                        return inetAddress;
                    }
                }
            }
            throw new IllegalStateException("The dedicated server has no bound TCP listener");
        }

        public DedicatedServer server() {
            DedicatedServer value = server.get();
            if (value == null) {
                throw new IllegalStateException("The dedicated server has not been created");
            }
            return value;
        }

        public void abort() {
            abortRequested.set(true);
            DedicatedServer value = server.get();
            if (value != null && value.isRunning()) {
                value.halt(false);
            }
        }

        private void capture(DedicatedServer value) {
            if (!server.compareAndSet(null, value) && server.get() != value) {
                fail(new IllegalStateException("A second dedicated server entered the active client test"));
                return;
            }
            if (abortRequested.get()) {
                value.halt(false);
            }
        }

        private void bootstrapReturned() {
            if (server.get() == null) {
                fail(new IllegalStateException(
                        "The dedicated-server entrypoint returned before creating a server"));
            }
        }

        private void bootstrapTerminated() {
            bootstrapTermination.complete(null);
        }

        private void serverTerminated() {
            if (!started.isDone()) {
                started.completeExceptionally(new IllegalStateException(
                        "The dedicated server terminated before startup completed"));
            }
            serverTermination.complete(null);
        }

        private void fail(Throwable failure) {
            started.completeExceptionally(failure);
            abort();
            if (server.get() == null) {
                serverTermination.complete(null);
            }
        }
    }
}
