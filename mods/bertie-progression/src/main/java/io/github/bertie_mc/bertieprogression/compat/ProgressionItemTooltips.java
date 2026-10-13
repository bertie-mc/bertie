package io.github.bertie_mc.bertieprogression.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = "bertieprogression", value = Dist.CLIENT)
public final class ProgressionItemTooltips {
    private ProgressionItemTooltips() {}

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        switch (BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()).toString()) {
            case "alexsmobs:shield_of_the_deep" -> {
                event.getToolTip().add(Component.translatable("tooltip.bertieprogression.breathing_stance"));
                event.getToolTip().add(Component.translatable("tooltip.bertieprogression.aquatic_reflect"));
            }
            case "cataclysm:azure_sea_shield" ->
                event.getToolTip().add(Component.translatable("tooltip.bertieprogression.perfect_parry"));
            case "alexsmobs:serrated_shark_tooth" ->
                event.getToolTip().add(Component.translatable("tooltip.bertieprogression.serrated_shark_tooth"));
            case "piglinproliferation:buckler" -> {
                var lines = event.getToolTip();
                for (int i = 0; i < lines.size(); i++) {
                    if (rangedBashDamage(lines.get(i)))
                        lines.set(i, Component.translatable("tooltip.bertieprogression.weapon_bash"));
                }
            }
            default -> {}
        }
    }

    private static boolean rangedBashDamage(Component component) {
        return containsKey(component, "attribute.piglinproliferation.ranged_modifier.")
                && containsKey(component, Attributes.ATTACK_DAMAGE.value().getDescriptionId());
    }

    private static boolean containsKey(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents contents) {
            if (contents.getKey().startsWith(key)) return true;
            for (Object argument : contents.getArgs()) {
                if (argument instanceof Component child && containsKey(child, key)) return true;
            }
        }
        return component.getSiblings().stream().anyMatch(child -> containsKey(child, key));
    }
}
