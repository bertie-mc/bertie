package com.berlord.bushtweaks.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces Berries & Cherries' contact damage with vanilla sweet-berry-bush
 * behavior. Every prickly block in that mod (berry bushes, grape trees, its
 * cherry/oak leaves, incl. picked variants) funnels through these two static
 * procedures, so this one mixin covers all of them.
 *
 * Differences from the mod's original logic:
 *  - crouching skips damage AND the slowdown (sneaking is slow enough)
 *  - no damage while standing still (vanilla's 0.003 movement gate)
 *  - sweet_berry_bush damage type instead of generic (proper death message)
 *  - vanilla stuck-in-bush slowdown while not crouching
 *  - vanilla immunities (foxes, bees) instead of the original's
 *    armor >= 1 and villager exemptions
 */
@Mixin(
        targets = {
                "net.mcreator.berriesandcherries.procedures.RaspberryBushDamageProcedure",
                "net.mcreator.berriesandcherries.procedures.BlueberryBushDamageProcedure"
        },
        remap = false
)
public class BerriesCherriesBushDamageMixin {

    @Inject(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void bushtweaks$vanillaBushBehavior(LevelAccessor world, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        if (entity.getType() == EntityType.FOX || entity.getType() == EntityType.BEE) {
            return;
        }
        if (entity.isSteppingCarefully()) {
            // keep the fall-damage cushion the slowdown would have given
            entity.resetFallDistance();
            return;
        }
        entity.makeStuckInBlock(Blocks.SWEET_BERRY_BUSH.defaultBlockState(), new Vec3(0.8D, 0.75D, 0.8D));
        if (!(world instanceof Level level) || level.isClientSide()) {
            return;
        }
        if (entity.xOld == entity.getX() && entity.zOld == entity.getZ()) {
            return;
        }
        double dx = Math.abs(entity.getX() - entity.xOld);
        double dz = Math.abs(entity.getZ() - entity.zOld);
        if (dx >= 0.003D || dz >= 0.003D) {
            entity.hurt(level.damageSources().sweetBerryBush(), 1.0F);
        }
    }
}
