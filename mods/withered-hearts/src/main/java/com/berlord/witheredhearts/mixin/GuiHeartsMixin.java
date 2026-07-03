package com.berlord.witheredhearts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Renders withered (dark) hearts only for the portion of health the Wither
 * effect will actually drain before it expires, instead of vanilla's
 * all-or-nothing dark health bar.
 *
 * <p>Wither deals 1 damage (half a heart) every {@code max(40 >> amplifier, 10)}
 * ticks, so the number of half-hearts it can still drain is
 * {@code duration / max(40 >> amplifier, 10)}. That count is computed once per
 * frame in {@link #witheredhearts$wrapForPlayer} and shared into
 * {@link #witheredhearts$wrapRenderHeart}, which paints exactly that many
 * leading half-hearts dark and leaves the rest the player's real (NORMAL) type.
 *
 * <p>Two MixinExtras {@code @WrapOperation} injections into {@code Gui#renderHearts}
 * mirror the original Fabric mod's behaviour, translated to Mojmap:
 * {@code InGameHud.HeartType.fromPlayerState -> Gui$HeartType.forPlayer} and
 * {@code InGameHud#drawHeart -> Gui#renderHeart}.
 */
@Mixin(Gui.class)
public class GuiHeartsMixin {

    /**
     * Wrap the per-frame heart-type lookup.
     *
     * <p>Computes how many half-hearts Wither will still drain and stashes it in
     * the shared {@code witheredCount} counter. Then, if vanilla resolved the bar
     * to {@code WITHERED} (which it does the instant the player has Wither), force
     * it back to {@code NORMAL} so vanilla stops painting the whole bar dark —
     * this mod repaints only the drained portion in injection #2.
     */
    @WrapOperation(
            method = "renderHearts",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui$HeartType;forPlayer(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/client/gui/Gui$HeartType;"
            )
    )
    private Gui.HeartType witheredhearts$wrapForPlayer(Player player,
                                                       Operation<Gui.HeartType> original,
                                                       @Share("witheredCount") LocalIntRef witheredCount) {
        Gui.HeartType type = original.call(player);

        MobEffectInstance wither = player.getEffect(MobEffects.WITHER);
        if (wither == null) {
            witheredCount.set(0);
        } else {
            // Ticks between each 1-damage hit; clamped to a 10-tick floor exactly
            // as the vanilla Wither effect tick logic does (40 >> amplifier).
            int step = Math.max(40 >> wither.getAmplifier(), 10);
            int duration = wither.isInfiniteDuration() ? Integer.MAX_VALUE : wither.getDuration();
            witheredCount.set(duration / step);
        }

        // Stop vanilla from darkening the entire bar; we draw the dark region ourselves.
        return type == Gui.HeartType.WITHERED ? Gui.HeartType.NORMAL : type;
    }

    /**
     * Wrap each individual heart draw.
     *
     * <p>Consumes the shared {@code witheredCount} from the start of the bar:
     * the first {@code witheredCount} half-hearts are drawn {@code WITHERED}
     * (dark), the remainder keep their real type. The counter decrements by 2 per
     * full heart and 1 per half. At the exact boundary (one half-heart of withered
     * health left over a full red heart) the original mod draws an extra flipped
     * withered half-heart so the dark region drains inside-out; for this tight,
     * asset-free build we draw a plain {@code WITHERED} heart at that boundary
     * instead — visually ~95% identical with zero textures.
     */
    @WrapOperation(
            method = "renderHearts",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;renderHeart(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Gui$HeartType;IIZZZ)V"
            )
    )
    private void witheredhearts$wrapRenderHeart(Gui self,
                                                GuiGraphics graphics,
                                                Gui.HeartType type,
                                                int x,
                                                int y,
                                                boolean hardcore,
                                                boolean blinking,
                                                boolean half,
                                                Operation<Void> original,
                                                @Share("witheredCount") LocalIntRef witheredCount) {
        int left = witheredCount.get();
        Gui.HeartType drawType;

        if (type == Gui.HeartType.CONTAINER || left <= 0) {
            // Empty heart container, or no withered health remaining: draw as-is.
            drawType = type;
        } else if (left == 1) {
            // Boundary: exactly one half-heart of withered health remains. Paint
            // this heart dark and exhaust the counter (no flipped boundary sprite
            // in this asset-free build).
            drawType = Gui.HeartType.WITHERED;
            witheredCount.set(0);
        } else {
            // Fully inside the withered region: paint dark and consume this heart's
            // worth of the counter (1 for a half heart, 2 for a full heart).
            drawType = Gui.HeartType.WITHERED;
            witheredCount.set(left - (half ? 1 : 2));
        }

        original.call(self, graphics, drawType, x, y, hardcore, blinking, half);
    }
}
