package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.ender.EyeAcquisitions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public abstract class EyeEnchantingMixin {
    @Shadow
    @Final
    public int[] costs;

    @Unique
    private int bertie$enchantingCost;

    @Unique
    private int bertie$previousSeed;

    @Inject(method = "clickMenuButton", at = @At("HEAD"))
    private void bertie$captureCost(Player player, int option, CallbackInfoReturnable<Boolean> cir) {
        bertie$enchantingCost = option >= 0 && option < costs.length ? costs[option] : 0;
        bertie$previousSeed = player.getEnchantmentSeed();
    }

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void bertie$crypticEye(Player player, int option, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && player.getEnchantmentSeed() != bertie$previousSeed) {
            EyeAcquisitions.enchanted(player, bertie$enchantingCost);
        }
    }
}
