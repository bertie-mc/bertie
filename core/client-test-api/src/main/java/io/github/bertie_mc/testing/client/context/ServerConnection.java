package io.github.bertie_mc.testing.client.context;

import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

/**
 * Client and server state joined by one test connection.
 *
 * <p>Unless otherwise specified, methods may only be called from the client-test thread.
 */
public interface ServerConnection {
    /** The default chunk download and rendering timeout, in ticks. */
    int DEFAULT_CHUNK_TIMEOUT_TICKS = SharedConstants.TICKS_PER_MINUTE;

    /**
     * Waits for every chunk in the effective client render distance to download.
     *
     * @return the number of ticks waited, possibly zero
     * @see #waitForChunksDownload(int)
     */
    default int waitForChunksDownload() {
        return waitForChunksDownload(DEFAULT_CHUNK_TIMEOUT_TICKS);
    }

    /**
     * Waits for every chunk in the effective client render distance to download. Client level
     * queries are reliable afterward, but the chunks may not have rendered yet.
     *
     * @param timeoutTicks a positive timeout in ticks, or {@link ClientTestContext#NO_TIMEOUT}
     * @return the number of ticks waited, possibly zero
     * @throws AssertionError if the chunks have not downloaded at the timeout
     */
    int waitForChunksDownload(int timeoutTicks);

    /**
     * Waits for expected chunks to download and for all present chunks to render.
     *
     * @return the number of ticks waited, possibly zero
     */
    default int waitForChunksRender() {
        return waitForChunksRender(true, DEFAULT_CHUNK_TIMEOUT_TICKS);
    }

    /**
     * Waits for expected chunks to download and for all present chunks to render.
     *
     * @param timeoutTicks a positive timeout in ticks, or {@link ClientTestContext#NO_TIMEOUT}
     * @return the number of ticks waited, possibly zero
     */
    default int waitForChunksRender(int timeoutTicks) {
        return waitForChunksRender(true, timeoutTicks);
    }

    /**
     * Waits for chunks to render, optionally waiting for every expected download first.
     *
     * @param waitForDownload whether every chunk in the effective render distance must download
     * @return the number of ticks waited, possibly zero
     */
    default int waitForChunksRender(boolean waitForDownload) {
        return waitForChunksRender(waitForDownload, DEFAULT_CHUNK_TIMEOUT_TICKS);
    }

    /**
     * Waits for chunks to render, optionally waiting for every expected download first. When
     * {@code waitForDownload} is false, this only guarantees that chunks already present on the
     * client have finished rendering.
     *
     * @param waitForDownload whether every chunk in the effective render distance must download
     * @param timeoutTicks a positive timeout in ticks, or {@link ClientTestContext#NO_TIMEOUT}
     * @return the number of ticks waited, possibly zero
     * @throws AssertionError if the requested state is not reached at the timeout
     */
    int waitForChunksRender(boolean waitForDownload, int timeoutTicks);

    /**
     * Waits until clientbound packets already sent by the server have been processed by the
     * client.
     *
     * <p>This does not make the server send a pending batched update. For example, wait one tick
     * after a block change before creating this barrier. Use
     * {@link #waitForClientboundEntityUpdates(EntityType, EntityType[])} for entity updates.
     */
    void waitForClientboundPackets();

    /** Waits until serverbound packets already sent by the client have been processed. */
    void waitForServerboundPackets();

    /**
     * Waits for the selected entity types' batched update intervals and then waits for all
     * clientbound packets sent before the resulting barrier.
     *
     * @param entityType the first entity type whose updates must be sent
     * @param additionalEntityTypes any additional entity types whose update intervals matter
     */
    void waitForClientboundEntityUpdates(EntityType<?> entityType, EntityType<?>... additionalEntityTypes);

    /**
     * Returns the client player.
     *
     * <p>Call only inside {@link ClientTestContext#runOnClient} or
     * {@link ClientTestContext#computeOnClient}.
     *
     * @return the connected client player
     */
    LocalPlayer clientPlayer();

    /**
     * Returns the client level.
     *
     * <p>Call only inside {@link ClientTestContext#runOnClient} or
     * {@link ClientTestContext#computeOnClient}.
     *
     * @return the connected client level
     */
    ClientLevel clientLevel();

    /**
     * Returns the corresponding server player.
     *
     * <p>Call only inside {@link ServerContext#runOnServer} or
     * {@link ServerContext#computeOnServer}.
     *
     * @return the corresponding server player
     */
    ServerPlayer serverPlayer();

    /**
     * Returns the corresponding server level.
     *
     * <p>Call only inside {@link ServerContext#runOnServer} or
     * {@link ServerContext#computeOnServer}.
     *
     * @return the corresponding server level
     */
    ServerLevel serverLevel();
}
