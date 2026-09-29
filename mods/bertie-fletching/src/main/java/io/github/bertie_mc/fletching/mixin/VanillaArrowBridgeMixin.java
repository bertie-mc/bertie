package io.github.bertie_mc.fletching.mixin;

import com.fletchery.mod.entity.CustomArrowEntity;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bertie_mc.fletching.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractArrow.class, remap = false)
public abstract class VanillaArrowBridgeMixin {
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = "onHitEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;isCritArrow()Z"))
    private boolean bertie$consistentCritical(boolean original) {
        // Component hits use one deterministic bow-critical multiplier, including flat bonuses.
        return bertie$arrow() == null && original;
    }

    @Unique
    private CustomArrowEntity bertie$arrow() {
        return (Object) this instanceof CustomArrowEntity arrow && ArrowProfile.redesigned(arrow.getCustomProperties())
                ? arrow
                : null;
    }

    @WrapOperation(
            method = "onHitEntity",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean bertie$rawDamage(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
        CustomArrowEntity arrow = bertie$arrow();
        LivingEntity living = ArrowDamage.living(target);
        if (arrow != null && living != null) {
            amount = ArrowDamage.raw(arrow, living, amount);
            if (((ArrowRuntime) arrow).bertie$flight().profile.shaft("ender")
                    && arrow.level() instanceof net.minecraft.server.level.ServerLevel server)
                source = ArrowDamage.source(server, "arrow", arrow, arrow.getOwner());
            var context = ArrowDamage.CURRENT.get();
            if (context != null) context.raw = amount;
        }
        return original.call(target, source, amount);
    }

    @WrapWithCondition(
            method = "onHitEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;discard()V"))
    private boolean bertie$keepForRicochet(AbstractArrow self) {
        CustomArrowEntity arrow = bertie$arrow();
        if (arrow == null) return true;
        FlightState state = ((ArrowRuntime) arrow).bertie$flight();
        return !state.tracing && state.ricochets >= state.profile.ricochets();
    }

    @WrapOperation(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private BlockHitResult bertie$phase(Level level, ClipContext context, Operation<BlockHitResult> original) {
        CustomArrowEntity arrow = bertie$arrow();
        if (arrow != null && ((ArrowRuntime) arrow).bertie$flight().profile.phase())
            return BlockHitResult.miss(context.getTo(), Direction.UP, BlockPos.containing(context.getTo()));
        return original.call(level, context);
    }

    @WrapOperation(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                            ordinal = 0))
    private BlockState bertie$insideBlock(Level level, BlockPos pos, Operation<BlockState> original) {
        CustomArrowEntity arrow = bertie$arrow();
        return arrow != null && ((ArrowRuntime) arrow).bertie$flight().profile.phase()
                ? Blocks.AIR.defaultBlockState()
                : original.call(level, pos);
    }

    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void bertie$visited(Entity target, CallbackInfoReturnable<Boolean> cir) {
        CustomArrowEntity arrow = bertie$arrow();
        if (arrow != null
                && (((ArrowRuntime) arrow).bertie$flight().hit.contains(target.getUUID())
                        || ArrowDamage.living(target) != null
                                && (!ArrowDamage.eligible(arrow.getOwner(), ArrowDamage.living(target))
                                        || ((ArrowRuntime) arrow)
                                                .bertie$flight()
                                                .hit
                                                .contains(ArrowDamage.living(target)
                                                        .getUUID())))) cir.setReturnValue(false);
    }
}
