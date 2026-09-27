package io.github.bertie_mc.spellrestrictions.mixin;

import io.github.bertie_mc.spellrestrictions.Restrictions;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ArcaneAnvilMenu.class, remap = false)
public abstract class ArcaneAnvilMenuMixin extends ItemCombinerMenu {
    protected ArcaneAnvilMenuMixin(MenuType<?> type, int id, Inventory inventory, ContainerLevelAccess access) {
        super(type, id, inventory, access);
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void validateUpgradeOutput(CallbackInfo ci) {
        if (!Restrictions.canTakeScroll(player, resultSlots.getItem(0))) resultSlots.setItem(0, ItemStack.EMPTY);
    }

    @Inject(method = "mayPickup", at = @At("RETURN"), cancellable = true)
    private void validatePickup(Player player, boolean hasStack, CallbackInfoReturnable<Boolean> cir) {
        if (!Restrictions.canTakeScroll(player, resultSlots.getItem(0))) cir.setReturnValue(false);
    }
}
