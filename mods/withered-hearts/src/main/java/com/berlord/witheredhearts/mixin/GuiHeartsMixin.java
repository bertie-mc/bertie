package com.berlord.witheredhearts.mixin;

import com.berlord.witheredhearts.logic.HeartRenderPolicy;
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

/** Draws only the half-hearts that the current Wither effect can still drain as dark. */
@Mixin(Gui.class)
public class GuiHeartsMixin {

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
            int duration = wither.isInfiniteDuration() ? Integer.MAX_VALUE : wither.getDuration();
            witheredCount.set(HeartRenderPolicy.remainingHalfHearts(duration, wither.getAmplifier()));
        }

        // Stop vanilla from darkening the entire bar; we draw the dark region ourselves.
        return type == Gui.HeartType.WITHERED ? Gui.HeartType.NORMAL : type;
    }

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
        HeartRenderPolicy.Draw draw = HeartRenderPolicy.nextDraw(
                type == Gui.HeartType.CONTAINER, witheredCount.get(), half);
        Gui.HeartType drawType = draw.withered() ? Gui.HeartType.WITHERED : type;
        witheredCount.set(draw.remainingHalfHearts());

        original.call(self, graphics, drawType, x, y, hardcore, blinking, half);
    }
}
