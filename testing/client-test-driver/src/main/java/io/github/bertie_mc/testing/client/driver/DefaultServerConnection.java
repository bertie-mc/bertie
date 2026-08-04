package io.github.bertie_mc.testing.client.driver;

import io.github.bertie_mc.testing.client.ClientTestContext;
import io.github.bertie_mc.testing.client.ServerConnection;
import io.github.bertie_mc.testing.client.driver.mixin.ClientChunkCacheAccessor;
import io.github.bertie_mc.testing.client.driver.mixin.ClientChunkCacheStorageAccessor;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.network.PacketDistributor;

class DefaultServerConnection implements ServerConnection {
    protected final DefaultClientTestContext context;
    protected final DefaultServerContext serverContext;
    private final UUID playerId;

    DefaultServerConnection(DefaultClientTestContext context, DefaultServerContext serverContext) {
        this.context = Objects.requireNonNull(context);
        this.serverContext = Objects.requireNonNull(serverContext);
        this.playerId = context.computeOnClient(client -> Objects.requireNonNull(
                        client.player, "The connected client player is not available")
                .getUUID());
    }

    @Override
    public void waitForChunksDownload() {
        waitForChunksDownload(DEFAULT_CHUNK_TIMEOUT_TICKS);
    }

    @Override
    public void waitForChunksDownload(int timeoutTicks) {
        int requiredDistance = requiredChunkDistance();
        context.waitFor(
                "client chunks to download",
                client -> areClientChunksDownloaded(client, requiredDistance),
                timeoutTicks);
    }

    @Override
    public void waitForChunksRender() {
        waitForChunksRender(DEFAULT_CHUNK_TIMEOUT_TICKS);
    }

    @Override
    public void waitForChunksRender(int timeoutTicks) {
        int requiredDistance = requiredChunkDistance();
        context.waitFor(
                "client chunks to download and render",
                client -> areClientChunksDownloaded(client, requiredDistance)
                        && areClientChunksRendered(client),
                timeoutTicks);
    }

    private int requiredChunkDistance() {
        return context.computeOnClient(client -> client.options.getEffectiveRenderDistance());
    }

    @Override
    public void waitForClientboundPackets() {
        ClientTestNetwork.Barrier barrier = ClientTestNetwork.newBarrier();
        serverContext.runOnServer(server -> PacketDistributor.sendToPlayer(serverPlayer(), barrier.payload()));
        waitForBarrier("clientbound packets", barrier);
    }

    @Override
    public void waitForServerboundPackets() {
        ClientTestNetwork.Barrier barrier = ClientTestNetwork.newBarrier();
        context.runOnClient(client -> PacketDistributor.sendToServer(barrier.payload()));
        waitForBarrier("serverbound packets", barrier);
    }

    private void waitForBarrier(String description, ClientTestNetwork.Barrier barrier) {
        try {
            context.waitForCondition(description, barrier.completion()::isDone);
            CompletableFuture<Void> completion = barrier.completion();
            if (completion.isCompletedExceptionally()) {
                context.await(description, completion);
            }
        } finally {
            ClientTestNetwork.discard(barrier);
        }
    }

    @Override
    public LocalPlayer clientPlayer() {
        requireClientThread();
        return Objects.requireNonNull(
                Minecraft.getInstance().player, "The client player is not available");
    }

    @Override
    public ClientLevel clientLevel() {
        requireClientThread();
        return Objects.requireNonNull(
                Minecraft.getInstance().level, "The client level is not available");
    }

    @Override
    public ServerPlayer serverPlayer() {
        requireServerThread();
        return Objects.requireNonNull(
                serverContext.server.getPlayerList().getPlayer(playerId),
                "The corresponding server player is not available");
    }

    @Override
    public ServerLevel serverLevel() {
        return serverPlayer().serverLevel();
    }

    private void requireClientThread() {
        context.requireClientThread();
    }

    private void requireServerThread() {
        if (!serverContext.server.isSameThread()) {
            throw new IllegalStateException(
                    "Server connection state must be accessed through ServerContext.runOnServer");
        }
    }

    private static boolean areClientChunksDownloaded(
            Minecraft client, int renderDistance) {
        ClientLevel level = client.level;
        if (level == null) {
            return false;
        }

        ClientChunkCache.Storage storage =
                ((ClientChunkCacheAccessor) level.getChunkSource()).bertie$getStorage();
        ClientChunkCacheStorageAccessor storageAccessor =
                (ClientChunkCacheStorageAccessor) (Object) storage;
        int centerX = storageAccessor.bertie$getViewCenterX();
        int centerZ = storageAccessor.bertie$getViewCenterZ();
        for (int z = centerZ - renderDistance; z <= centerZ + renderDistance; z++) {
            for (int x = centerX - renderDistance; x <= centerX + renderDistance; x++) {
                if (level.getChunk(x, z, ChunkStatus.FULL, false) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean areClientChunksRendered(Minecraft client) {
        ClientLevel level = client.level;
        return level != null
                && level.isLightUpdateQueueEmpty()
                && client.levelRenderer.hasRenderedAllSections();
    }
}
