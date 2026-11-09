package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.fountain.FountainNeighbors;
import io.github.bertie_mc.bertieprogression.fountain.MineralFountainRules;
import io.github.bertie_mc.bertieprogression.fountain.SturdyDeepslateRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.dubhe.anvilcraft.block.entity.MineralFountainBlockEntity", remap = false)
public abstract class MineralFountainMixin {
    @Unique
    private final FountainNeighbors bertieprogression$neighbors = new FountainNeighbors();

    @Redirect(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ldev/dubhe/anvilcraft/block/entity/MineralFountainBlockEntity;getAroundBlock()Lnet/minecraft/world/level/block/state/BlockState;"),
            require = 1)
    private BlockState bertieprogression$cascade(@Coerce Object fountain) {
        BlockEntity entity = (BlockEntity) fountain;
        if (!(entity.getLevel() instanceof ServerLevel level)) return Blocks.AIR.defaultBlockState();
        var neighbors = bertieprogression$neighbors.get(level.getGameTime(), direction -> {
            var pos = entity.getBlockPos().relative(direction);
            return level.hasChunkAt(pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
        });
        // Let the native tick finish its heater cleanup after the new result replaces Deepslate.
        return MineralFountainRules.process(level, entity.getBlockPos(), neighbors)
                ? Blocks.AIR.defaultBlockState()
                : FountainNeighbors.common(neighbors);
    }

    @ModifyArg(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"),
            index = 1,
            require = 1)
    private BlockState bertieprogression$defaultSturdy(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals("anvilcraft:cinerite")
                ? MineralFountainRules.block(SturdyDeepslateRules.ID)
                : state;
    }
}
