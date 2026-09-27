package io.github.bertie_mc.spellrestrictions.mixin;

import io.github.bertie_mc.spellrestrictions.Restrictions;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu$4", remap = false)
public abstract class ScrollForgeResultSlotMixin extends SlotItemHandler {
    public ScrollForgeResultSlotMixin(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    @Override
    public boolean mayPickup(Player player) {
        return super.mayPickup(player) && Restrictions.canTakeScroll(player, getItem());
    }
}
