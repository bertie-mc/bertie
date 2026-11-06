package io.github.bertie_mc.bertieprogression.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "dev.dubhe.anvilcraft.block.entity.OverseerBlockEntity", remap = false)
public abstract class OverseerSingleChunkMixin {
    @ModifyArg(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ldev/dubhe/anvilcraft/api/world/load/LoadChuckData;createLoadChuckData(ILnet/minecraft/core/BlockPos;ZLnet/minecraft/server/level/ServerLevel;)Ldev/dubhe/anvilcraft/api/world/load/LoadChuckData;"),
            index = 0,
            require = 1)
    private int bertieprogression$singleChunkRadius(int radius) {
        return 0;
    }
}
