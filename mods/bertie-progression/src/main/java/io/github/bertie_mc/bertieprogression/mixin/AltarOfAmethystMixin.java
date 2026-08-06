package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.altar.AltarOfAmethystRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies {@link AltarOfAmethystRules} to Cataclysm's Altar of Amethyst.
 *
 * <p>Targeted by STRING: Cataclysm is an optional runtime dependency and is not on this module's
 * compile classpath, so there is no {@code AltarOfAmethyst_Block_Entity} type to name here.
 *
 * <p>The altar parameter carries {@link Coerce} because the target method's descriptor contains
 * Cataclysm's concrete block-entity class. Coercion lets the injector bind that optional subclass
 * to its {@link BlockEntity} supertype without adding Cataclysm to the compile classpath.
 *
 * <p>Listed in the common {@code mixins} array so the same injector applies on both physical sides.
 * The client and server must advance and gate altar progress consistently.
 */
@Mixin(targets = "com.github.L_Ender.cataclysm.blockentities.AltarOfAmethyst_Block_Entity", remap = false)
public abstract class AltarOfAmethystMixin {

    /**
     * Stop the tick outright when the altar should not run at all - daytime, or no line of sight to
     * the sky outside a lush cave.
     */
    @Inject(method = "cookingTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void bertieprogression$gate(
            Level level, BlockPos pos, BlockState state, @Coerce BlockEntity altar, CallbackInfo ci) {
        // Run the deterministic rule on both sides. If only the server cancels, the client keeps
        // advancing and briefly renders progress and a beam that the next sync packet removes.
        if (AltarOfAmethystRules.speedAt(level, pos) == AltarOfAmethystRules.STOPPED) {
            ci.cancel();
        }
    }

    /**
     * Cataclysm has already advanced the counter by one this tick; add the rest of the multiplier
     * on top, clamped so a fast altar cannot overshoot its own total.
     */
    @Inject(method = "cookingTick", at = @At("TAIL"), remap = false)
    private static void bertieprogression$accelerate(
            Level level, BlockPos pos, BlockState state, @Coerce BlockEntity altar, CallbackInfo ci) {
        // Both sides, for the same reason as the gate: a client that advances at a different rate
        // than the server renders progress the server does not have.
        float speed = AltarOfAmethystRules.speedAt(level, pos);
        if (speed <= 1.0F) {
            return;
        }
        AltarOfAmethystAccessor access = (AltarOfAmethystAccessor) altar;
        int current = access.bertieprogression$cookingTime();
        // Nothing in progress: leave an idle altar's counter alone.
        if (current <= 0) {
            return;
        }
        int total = access.bertieprogression$cookingTimeTotal();
        access.bertieprogression$setCookingTime(Math.min(current + (Math.round(speed) - 1), total));
    }
}
