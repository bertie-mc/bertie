package io.github.bertie_mc.spellrestrictions.mixin;

import io.github.bertie_mc.spellrestrictions.RestrictionConfig;
import io.github.bertie_mc.spellrestrictions.Restrictions;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import io.redspace.ironsspellbooks.item.InkItem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractSpell.class, remap = false)
public abstract class AbstractSpellMixin {
    @Inject(method = "requiresLearning", at = @At("RETURN"), cancellable = true)
    private void specialSchoolsRequireResearch(CallbackInfoReturnable<Boolean> cir) {
        if (Restrictions.isSpecial((AbstractSpell) (Object) this)) cir.setReturnValue(true);
    }

    @Inject(method = "canBeCraftedBy", at = @At("RETURN"), cancellable = true)
    private void showCraftingGates(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || player == null) return;
        var spell = (AbstractSpell) (Object) this;
        if (RestrictionConfig.REQUIRE_DISCOVERY.get() && !Restrictions.knows(player, spell)) {
            cir.setReturnValue(false);
        } else if (player.containerMenu instanceof ScrollForgeMenu menu
                && menu.getInkSlot().getItem().getItem() instanceof InkItem ink) {
            cir.setReturnValue(Restrictions.canCraft(player, spell, spell.getMinLevelForRarity(ink.getRarity())));
        }
    }
}
