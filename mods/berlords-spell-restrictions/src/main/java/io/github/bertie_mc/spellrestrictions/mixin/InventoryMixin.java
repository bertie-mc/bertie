package io.github.bertie_mc.spellrestrictions.mixin;

import io.github.bertie_mc.spellrestrictions.Restrictions;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow
    @Final
    public Player player;

    @Shadow
    public abstract ItemStack getItem(int slot);

    @Inject(method = "setItem", at = @At("RETURN"))
    private void discoverSetSlot(int slot, ItemStack stack, CallbackInfo ci) {
        Restrictions.observe(player, stack);
    }

    @Inject(method = "addResource(ILnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"))
    private void discoverInsertedStack(int slot, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        Restrictions.observe(player, getItem(slot));
    }
}
