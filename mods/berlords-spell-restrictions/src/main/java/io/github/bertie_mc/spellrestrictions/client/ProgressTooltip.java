package io.github.bertie_mc.spellrestrictions.client;

import io.github.bertie_mc.spellrestrictions.RestrictionConfig;
import io.github.bertie_mc.spellrestrictions.Restrictions;
import io.github.bertie_mc.spellrestrictions.SpellRestrictions;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = SpellRestrictions.ID, value = Dist.CLIENT)
public final class ProgressTooltip {
    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        var player = event.getEntity();
        if (player == null || !(event.getItemStack().getItem() instanceof Scroll)) return;
        var data = ISpellContainer.get(event.getItemStack()).getSpellAtIndex(0);
        if (RestrictionConfig.REQUIRE_DISCOVERY.get()) {
            boolean known = Restrictions.knows(player, data.getSpell());
            event.getToolTip()
                    .add(Component.translatable(
                                    "tooltip.berlordsspellrestrictions." + (known ? "discovered" : "undiscovered"))
                            .withStyle(known ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY));
        }
        if (RestrictionConfig.REQUIRE_RARITY.get()
                && data.getRarity().getValue() > Restrictions.state(player).tier()) {
            event.getToolTip()
                    .add(Component.translatable(
                                    "tooltip.berlordsspellrestrictions.rarity_locked",
                                    data.getRarity().getDisplayName())
                            .withStyle(ChatFormatting.GRAY));
        }
    }
}
