package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.altar.AltarOfAmethystRules;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Applies {@link AltarOfAmethystRules} to Cataclysm's Altar of Amethyst.
 *
 * <p>Targeted by STRING, not by class literal: Cataclysm is an optional runtime dependency and is
 * not on this module's compile classpath, so there is no {@code AltarOfAmethyst_Block_Entity} type
 * to reference here. The mixin config's {@code required: false} means a pack without Cataclysm
 * simply never applies it.
 *
 * <p>Listed in the common {@code mixins} array of the config, never in {@code client}/{@code
 * server} - NeoForge silently fails to apply those sub-array entries (docs/gotchas.md, learned via
 * explosive-enhancement 2026-06-14). This is server-side logic anyway: cooking runs on the logical
 * server.
 *
 * <p>{@code cookingTick} is static and takes the block entity as its last parameter, so the speed
 * work is done through {@link Accessor} rather than on {@code this}.
 */
@Mixin(targets = "com.github.L_Ender.cataclysm.blockentities.AltarOfAmethyst_Block_Entity",
        remap = false)
public abstract class AltarOfAmethystMixin {

    /**
     * Cataclysm's own progress counter. Vanilla-for-that-mod behaviour is +1 per tick until it
     * reaches {@code cookingTimeTotal}; a multiplier here means adding the extra on top.
     */
    @Accessor("cookingTime")
    public abstract int bertieprogression$cookingTime();

    @Accessor("cookingTime")
    public abstract void bertieprogression$setCookingTime(int value);

    @Accessor("cookingTimeTotal")
    public abstract int bertieprogression$cookingTimeTotal();

    /**
     * Stop the tick outright when the altar should not be running at all - daytime, or no line of
     * sight to the sky outside a lush cave.
     */
    @Inject(method = "cookingTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void bertieprogression$gate(Level level, BlockPos pos, BlockState state,
            BlockEntity altar, CallbackInfo ci) {
        if (level.isClientSide) {
            return;
        }
        if (AltarOfAmethystRules.speedAt(level, pos) == AltarOfAmethystRules.STOPPED) {
            ci.cancel();
        }
    }

    /**
     * Cataclysm has already advanced the counter by one this tick; add the remainder of the
     * multiplier on top, clamped so a fast altar cannot overshoot its own total.
     */
    @Inject(method = "cookingTick", at = @At("TAIL"), remap = false)
    private static void bertieprogression$accelerate(Level level, BlockPos pos, BlockState state,
            BlockEntity altar, CallbackInfo ci) {
        if (level.isClientSide) {
            return;
        }
        float speed = AltarOfAmethystRules.speedAt(level, pos);
        if (speed <= 1.0F) {
            return;
        }
        AltarOfAmethystMixin access = (AltarOfAmethystMixin) (Object) altar;
        int current = access.bertieprogression$cookingTime();
        // Nothing in progress: leave it alone rather than pushing an idle altar's counter up.
        if (current <= 0) {
            return;
        }
        int total = access.bertieprogression$cookingTimeTotal();
        int extra = Math.round(speed) - 1;
        access.bertieprogression$setCookingTime(Math.min(current + extra, total));
    }
}
