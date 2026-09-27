package io.github.bertie_mc.enchantingrerollcost.mixin;

import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu;
import io.github.bertie_mc.enchantingrerollcost.RerollPrice;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Apply after the compatibility mixin so this HEAD handler precedes its fixed-price handler.
@Mixin(value = ApothEnchantmentMenu.class, priority = 900)
public abstract class ApothRerollMixin {
    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void rerollCost$charge(Player player, int button, CallbackInfoReturnable<Boolean> callback) {
        if (button == 3) callback.setReturnValue(RerollPrice.reroll((ApothEnchantmentMenu) (Object) this, player));
    }
}
