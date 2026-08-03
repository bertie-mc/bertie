package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.altar.AltarOfAmethystRules;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Applies {@link AltarOfAmethystRules} to Cataclysm's Altar of Amethyst.
 *
 * <p>Targeted by STRING: Cataclysm is an optional runtime dependency and is not on this module's
 * compile classpath, so there is no {@code AltarOfAmethyst_Block_Entity} type to name here.
 *
 * <p><b>That is also why the altar parameter is {@link Coerce} {@code Object}.</b> Mixin matches an
 * injector against the target method's EXACT descriptor, and declaring the parameter as its real
 * supertype {@code BlockEntity} is not close enough - the whole mixin is refused:
 * <pre>
 * InvalidInjectionException: Invalid descriptor ... Expected (...AltarOfAmethyst_Block_Entity...)
 *                                                   but found (...BlockEntity...)
 * </pre>
 * That refusal took the altar's block entity down with it and its UI stopped opening (2026-08-04).
 * {@code @Coerce} is the supported way to accept a type you cannot reference.
 *
 * <p>Listed in the common {@code mixins} array, never in {@code client}/{@code server} - NeoForge
 * silently fails to apply those sub-array entries (docs/gotchas.md, explosive-enhancement
 * 2026-06-14). Cooking runs on the logical server anyway.
 */
@Mixin(targets = "com.github.L_Ender.cataclysm.blockentities.AltarOfAmethyst_Block_Entity",
        remap = false)
public abstract class AltarOfAmethystMixin {

    /**
     * Stop the tick outright when the altar should not run at all - daytime, or no line of sight to
     * the sky outside a lush cave.
     */
    @Inject(method = "cookingTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void bertieprogression$gate(Level level, BlockPos pos, BlockState state,
            @Coerce Object altar, CallbackInfo ci) {
        // Runs on BOTH sides deliberately. Skipping the client let it keep ticking while the
        // server was cancelled: the client ran the craft to completion and drew the altar's beam
        // for a few seconds until a sync packet reset it (berlord 2026-08-04). Every input here -
        // day time, sky access, biome, moon phase - is available client-side, so both sides agree.
        if (AltarOfAmethystRules.speedAt(level, pos) == AltarOfAmethystRules.STOPPED) {
            ci.cancel();
        }
    }

    /**
     * Cataclysm has already advanced the counter by one this tick; add the rest of the multiplier
     * on top, clamped so a fast altar cannot overshoot its own total.
     */
    @Inject(method = "cookingTick", at = @At("TAIL"), remap = false)
    private static void bertieprogression$accelerate(Level level, BlockPos pos, BlockState state,
            @Coerce Object altar, CallbackInfo ci) {
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
        access.bertieprogression$setCookingTime(
                Math.min(current + (Math.round(speed) - 1), total));
    }
}
