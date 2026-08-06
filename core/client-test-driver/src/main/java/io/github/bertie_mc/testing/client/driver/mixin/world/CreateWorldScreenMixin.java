package io.github.bertie_mc.testing.client.driver.mixin.world;

import io.github.bertie_mc.testing.client.driver.world.DedicatedWorldPreparation;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Diverts create-world persistence when preparing a dedicated client-test world. */
@Mixin(net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.class)
abstract class CreateWorldScreenMixin {
    @Redirect(
            method = "createNewWorld",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;createLevelFromExistingSettings(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/server/ReloadableServerResources;Lnet/minecraft/core/LayeredRegistryAccess;Lnet/minecraft/world/level/storage/WorldData;)V"))
    private void bertie$writeDedicatedWorld(
            WorldOpenFlows flows,
            LevelStorageSource.LevelStorageAccess storage,
            ReloadableServerResources resources,
            LayeredRegistryAccess<RegistryLayer> registries,
            WorldData worldData) {
        if (!DedicatedWorldPreparation.intercept(storage, registries, worldData)) {
            flows.createLevelFromExistingSettings(storage, resources, registries, worldData);
        }
    }
}
