package io.github.bertie_mc.testing.client;

import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Client and server state joined by one test connection. */
public interface ServerConnection {
    int DEFAULT_CHUNK_TIMEOUT_TICKS = SharedConstants.TICKS_PER_MINUTE;

    void waitForChunksDownload();

    void waitForChunksDownload(int timeoutTicks);

    void waitForChunksRender();

    void waitForChunksRender(int timeoutTicks);

    void waitForClientboundPackets();

    void waitForServerboundPackets();

    /** Returns the client player. Call from {@link ClientTestContext#runOnClient}. */
    LocalPlayer clientPlayer();

    /** Returns the client level. Call from {@link ClientTestContext#runOnClient}. */
    ClientLevel clientLevel();

    /** Returns the corresponding server player. Call from {@link ServerContext#runOnServer}. */
    ServerPlayer serverPlayer();

    /** Returns the corresponding server level. Call from {@link ServerContext#runOnServer}. */
    ServerLevel serverLevel();
}
