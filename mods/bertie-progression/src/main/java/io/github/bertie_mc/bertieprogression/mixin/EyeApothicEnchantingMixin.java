package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.ender.EyeAcquisitions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu", remap = false)
public abstract class EyeApothicEnchantingMixin {
    @Unique
    private int bertie$requiredLevel;

    @Unique
    private int bertie$previousSeed;

    @Inject(method = "clickMenuButton", at = @At("HEAD"), require = 1)
    private void bertie$capture(Player player, int option, CallbackInfoReturnable<Boolean> cir) {
        int[] costs = ((EnchantmentMenu) (Object) this).costs;
        bertie$requiredLevel = option >= 0 && option < costs.length ? costs[option] : 0;
        bertie$previousSeed = player.getEnchantmentSeed();
    }

    @Inject(method = "clickMenuButton", at = @At("RETURN"), require = 1)
    private void bertie$successfulEnchant(Player player, int option, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && player.getEnchantmentSeed() != bertie$previousSeed) {
            EyeAcquisitions.enchanted(player, bertie$requiredLevel);
        }
    }
}
