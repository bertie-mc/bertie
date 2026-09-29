package io.github.bertie_mc.fletching.mixin;

import io.github.bertie_mc.fletching.ArrowStatus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = LivingEntity.class, remap = false)
public abstract class ArrowMovementEffectMixin {
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3 bertie$movement(Vec3 movement) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target.hasEffect(ArrowStatus.FROZEN)) {
            target.setDeltaMovement(Vec3.ZERO);
            return Vec3.ZERO;
        }
        return target.hasEffect(ArrowStatus.FRIGHTENED) ? new Vec3(-movement.x, movement.y, -movement.z) : movement;
    }
}
