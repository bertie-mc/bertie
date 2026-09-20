package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.TrinketConversions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps a demoted helmet out of the armour slots.
 *
 * <p>Everything that decides whether a stack may sit in an armour slot — the slot's own
 * {@code mayPlace}, shift-clicking, a mob picking gear up — resolves through this one static
 * method, so answering {@code MAINHAND} for the item is enough to close all of them at once.
 */
@Mixin(LivingEntity.class)
public abstract class HeadTrinketSlotMixin {

    @Inject(method = "getEquipmentSlotForItem", at = @At("HEAD"), cancellable = true)
    private static void bertie$keepDemotedHelmetsOutOfArmourSlots(
            ItemStack stack, CallbackInfoReturnable<EquipmentSlot> cir) {
        if (TrinketConversions.blocksArmourSlot(stack.getItem())) {
            cir.setReturnValue(EquipmentSlot.MAINHAND);
        }
    }
}
