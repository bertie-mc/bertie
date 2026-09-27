package io.github.bertie_mc.enchantingrerollcost.mixin.client;

import com.kasch_x.easyapothcompat.EasyApothConfig;
import com.kasch_x.easyapothcompat.client.RerollButton;
import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu;
import io.github.bertie_mc.enchantingrerollcost.RerollPrice;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RerollButton.class)
public abstract class RerollButtonMixin {
    @Shadow
    @Final
    private ApothEnchantmentMenu menu;

    @Redirect(
            method = "renderWidget",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/neoforged/neoforge/common/ModConfigSpec$IntValue;get()Ljava/lang/Object;"),
            require = 2)
    private Object rerollCost$displayPrice(ModConfigSpec.IntValue setting) {
        if (setting == EasyApothConfig.REROLL_LAPIS_COST) return 1;
        return RerollPrice.experienceCost(menu, Minecraft.getInstance().player);
    }

    @Inject(method = "canReroll", at = @At("HEAD"), cancellable = true)
    private void rerollCost$affordability(CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(RerollPrice.canReroll(menu, Minecraft.getInstance().player));
    }
}
