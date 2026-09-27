package io.github.bertie_mc.bertieprogression.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Tiny Skeletons turns a spawning skeleton into its baby on one shared 5% roll. A wither skeleton
 * additionally needs a coin flip, which halves how often baby wither skeletons appear.
 */
@Pseudo
@Mixin(targets = "fuzs.tinyskeletons.handler.BabyConversionHandler", remap = false)
public abstract class BabyWitherSkeletonOddsMixin {
    @WrapOperation(
            method = "onEntitySpawn",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/monster/Zombie;getSpawnAsBabyOdds(Lnet/minecraft/util/RandomSource;)Z"),
            require = 1)
    private static boolean bertieprogression$halveWitherBabies(
            RandomSource random, Operation<Boolean> original, @Local(argsOnly = true) Entity entity) {
        return original.call(random) && (entity.getType() != EntityType.WITHER_SKELETON || random.nextBoolean());
    }
}
