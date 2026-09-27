package io.github.bertie_mc.spellrestrictions.mixin;

import io.github.bertie_mc.spellrestrictions.Restrictions;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import io.redspace.ironsspellbooks.item.InkItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ScrollForgeMenu.class, remap = false)
public abstract class ScrollForgeMenuMixin {
    @Shadow
    @Final
    private Slot inkSlot;

    @Shadow
    @Final
    private Slot resultSlot;

    @Unique
    private Player berlords$owner;

    @Inject(
            method =
                    "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            at = @At("RETURN"))
    private void rememberOwner(int id, Inventory inventory, BlockEntity entity, CallbackInfo ci) {
        berlords$owner = inventory.player;
    }

    @Inject(method = "setupResultSlot", at = @At("HEAD"), cancellable = true)
    private void validateResult(AbstractSpell spell, CallbackInfo ci) {
        if (berlords$owner != null
                && inkSlot.getItem().getItem() instanceof InkItem ink
                && (spell.getMinRarity() > ink.getRarity().getValue()
                        || !Restrictions.canCraft(
                                berlords$owner, spell, spell.getMinLevelForRarity(ink.getRarity())))) {
            resultSlot.set(ItemStack.EMPTY);
            ci.cancel();
        }
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void validateShiftClick(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index == 39 && !Restrictions.canTakeScroll(player, resultSlot.getItem()))
            cir.setReturnValue(ItemStack.EMPTY);
    }
}
