package io.github.bertie_mc.spellrestrictions.mixin;

import io.github.bertie_mc.spellrestrictions.Restrictions;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryChangeTrigger.class)
public abstract class InventoryChangeTriggerMixin {
    // Vanilla emits this only for changed player slots; no additional tick scan.
    @Inject(
            method =
                    "trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"))
    private void discoverChangedSlot(ServerPlayer player, Inventory inventory, ItemStack stack, CallbackInfo ci) {
        Restrictions.observe(player, stack);
    }
}
