package com.berlord.foodsystem.mixin;

import com.berlord.foodsystem.buffs.ActiveFoods;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** durability_saver ability: chance for held tools to take no durability damage. */
@Mixin(ItemStack.class)
public abstract class ItemStackDurabilityMixin {

    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void bfs$saveDurability(int amount, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {
        if (!(entity instanceof Player player) || player.level().isClientSide()) return;
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) return;
        double chance = ActiveFoods.durabilitySaverChance(player);
        if (chance > 0 && player.getRandom().nextDouble() < chance) {
            ci.cancel();
        }
    }
}
