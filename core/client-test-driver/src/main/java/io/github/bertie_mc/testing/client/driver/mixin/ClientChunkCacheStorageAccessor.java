package io.github.bertie_mc.testing.client.driver.mixin;

import net.minecraft.client.multiplayer.ClientChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientChunkCache.Storage.class)
public interface ClientChunkCacheStorageAccessor {
    @Accessor("viewCenterX")
    int bertie$getViewCenterX();

    @Accessor("viewCenterZ")
    int bertie$getViewCenterZ();
}
