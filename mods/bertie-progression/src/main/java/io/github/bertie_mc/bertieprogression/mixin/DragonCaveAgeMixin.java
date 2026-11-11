package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.dragon.DragonProgression;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "com.iafenvoy.iceandfire.world.structure.DragonCaveStructure$DragonCavePiece", remap = false)
public abstract class DragonCaveAgeMixin {
    @ModifyVariable(method = "createDragon", at = @At("HEAD"), argsOnly = true, require = 1)
    private int bertie$limitGeneratedDragonAge(
            int value, WorldGenLevel world, RandomSource random, BlockPos position, int age) {
        return DragonProgression.generatedAge(
                value, world.getLevel().dimension().location());
    }
}
