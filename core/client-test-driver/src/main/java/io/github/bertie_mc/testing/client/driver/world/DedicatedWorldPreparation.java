package io.github.bertie_mc.testing.client.driver.world;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;

/** Bridge between the client create-world screen and a dedicated-server save. */
public final class DedicatedWorldPreparation {
    private static final AtomicReference<Request> ACTIVE = new AtomicReference<>();

    private DedicatedWorldPreparation() {}

    static Request begin() {
        Request request = new Request();
        if (!ACTIVE.compareAndSet(null, request)) {
            throw new IllegalStateException("A dedicated world is already being prepared");
        }
        return request;
    }

    public static boolean intercept(
            LevelStorageSource.LevelStorageAccess storage,
            LayeredRegistryAccess<RegistryLayer> registries,
            WorldData worldData) {
        Request request = ACTIVE.getAndSet(null);
        if (request == null) {
            return false;
        }

        Path levelPath =
                storage.getLevelPath(LevelResource.ROOT).toAbsolutePath().normalize();
        Path universe = storage.parent().getBaseDir().toAbsolutePath().normalize();
        String levelName = storage.getLevelId();
        try {
            storage.saveDataTag(registries.compositeAccess(), worldData);
            storage.close();
            if (!Files.isRegularFile(levelPath.resolve("level.dat"))) {
                throw new IllegalStateException("Minecraft did not write level.dat for " + levelPath);
            }
            request.result.complete(new PreparedDedicatedWorld(universe, levelName));
        } catch (Throwable failure) {
            storage.safeClose();
            request.result.completeExceptionally(failure);
        }
        return true;
    }

    static final class Request implements AutoCloseable {
        private final CompletableFuture<PreparedDedicatedWorld> result = new CompletableFuture<>();

        CompletableFuture<PreparedDedicatedWorld> result() {
            return result;
        }

        @Override
        public void close() {
            if (ACTIVE.compareAndSet(this, null)) {
                result.completeExceptionally(
                        new IllegalStateException("Dedicated world preparation ended before world data was written"));
            }
        }
    }
}
