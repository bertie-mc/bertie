package io.github.bertie_mc.testing.client.driver.mixin.context;

import net.minecraft.client.multiplayer.ClientChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the chunk-cache view center used to determine expected downloads. */
@Mixin(ClientChunkCache.Storage.class)
public interface ClientChunkCacheStorageAccessor {
    @Accessor("viewCenterX")
    int bertie$getViewCenterX();

    @Accessor("viewCenterZ")
    int bertie$getViewCenterZ();
}
