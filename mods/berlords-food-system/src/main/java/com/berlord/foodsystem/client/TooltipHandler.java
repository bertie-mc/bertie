package com.berlord.foodsystem.client;

import com.berlord.foodsystem.BFS;
import com.berlord.foodsystem.buffs.BuffsConfig;
import com.berlord.foodsystem.stomach.Stomach;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Food tooltips: hearts/regen/minutes the food provides in a stomach slot, an automatic
 * description of every configured buff (effects, attributes, abilities), and the Eternal tag.
 */
@EventBusSubscriber(modid = BFS.MODID, value = Dist.CLIENT)
public final class TooltipHandler {

    private static final DecimalFormat HEARTS = new DecimalFormat("#0.#");
    private static final DecimalFormat AMOUNT = new DecimalFormat("#0.##");
    /** desaturated, cool lavender for everything eternal */
    private static final int ETERNAL_COLOR = 0xA8A0D8;

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        var food = stack.getFoodProperties(null);
        boolean isSystemFood = food != null && food.nutrition() > 0 && food.saturation() > 0 && !stack.is(Stomach.NOT_FOOD);

        if (isSystemFood) {
            Stomach.FoodStats stats = Stomach.foodStats(stack);
            tooltip.add(Component.literal("❤ " + HEARTS.format(stats.healthBoost() * 0.5) + " Hearts").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("☀ " + Math.round(stats.regenFactor()) + " Regen").withStyle(ChatFormatting.DARK_RED));
            if (stack.has(BFS.ETERNAL.get())) {
                tooltip.add(Component.literal("⌚ ∞").withStyle(s -> s.withColor(ETERNAL_COLOR)));
            } else {
                tooltip.add(Component.literal("⌚ " + Math.round(stats.duration() / 1200.0) + " Minutes").withStyle(ChatFormatting.GOLD));
            }
            appendBuffLines(stack, tooltip);
            if (stack.is(Stomach.ALWAYS_EDIBLE)) {
                tooltip.add(Component.literal("Always edible").withStyle(ChatFormatting.GRAY));
            }
        } else if (stack.is(Stomach.SLICEABLE)) {
            tooltip.add(Component.literal("Heals 2 ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("❤ ").withStyle(ChatFormatting.RED))
                    .append(Component.literal("per slice").withStyle(ChatFormatting.GRAY)));
        }

        if (stack.has(BFS.ETERNAL.get())) {
            tooltip.add(Component.translatable("tooltip.berlords_food_system.eternal")
                    .withStyle(s -> s.withColor(ETERNAL_COLOR).withItalic(true)));
        }

        if (com.berlord.foodsystem.compat.SBCompat.isAdvancedFeedingUpgrade(stack)) {
            tooltip.add(Component.translatable("tooltip.berlords_food_system.disabled_item")
                    .withStyle(ChatFormatting.RED));
        }
    }

    /** automatic "what does this food grant" lines from the buffs config */
    private static void appendBuffLines(ItemStack stack, List<Component> tooltip) {
        BuffsConfig.FoodBuff buff = BuffsConfig.get().foods.get(stack.getItem());
        if (buff == null) return;

        for (BuffsConfig.EffectDef def : buff.effects) {
            MutableComponent name = Component.translatable(def.effect().value().getDescriptionId());
            if (def.amplifier() > 0) {
                name = Component.translatable("potion.withAmplifier", name,
                        Component.translatable("potion.potency." + def.amplifier()));
            }
            ChatFormatting color = def.effect().value().getCategory() == MobEffectCategory.HARMFUL
                    ? ChatFormatting.RED : ChatFormatting.BLUE;
            tooltip.add(Component.literal("✦ ").append(name).withStyle(color));
        }

        for (BuffsConfig.AttributeDef def : buff.attributes) {
            AttributeModifier mod = def.modifier();
            boolean positive = mod.amount() > 0;
            boolean percent = mod.operation() != AttributeModifier.Operation.ADD_VALUE;
            double shown = Math.abs(percent ? mod.amount() * 100 : mod.amount());
            String sign = positive ? "+" : "-";
            MutableComponent line = Component.literal("✦ " + sign + AMOUNT.format(shown) + (percent ? "% " : " "))
                    .append(Component.translatable(def.attribute().value().getDescriptionId()));
            tooltip.add(line.withStyle(positive ? ChatFormatting.BLUE : ChatFormatting.RED));
        }

        var ab = buff.abilities;
        if (ab.flight) tooltip.add(Component.translatable("tooltip.berlords_food_system.ability.flight").withStyle(ChatFormatting.AQUA));
        if (ab.climbing) tooltip.add(Component.translatable("tooltip.berlords_food_system.ability.climbing").withStyle(ChatFormatting.AQUA));
        if (ab.endermanCalm) tooltip.add(Component.translatable("tooltip.berlords_food_system.ability.enderman_calm").withStyle(ChatFormatting.AQUA));
        if (ab.magnetRadius > 0) tooltip.add(Component.translatable("tooltip.berlords_food_system.ability.magnet",
                AMOUNT.format(ab.magnetRadius)).withStyle(ChatFormatting.AQUA));
        if (ab.xpBoost != 1.0) tooltip.add(Component.translatable("tooltip.berlords_food_system.ability.xp_boost",
                AMOUNT.format(ab.xpBoost)).withStyle(ChatFormatting.AQUA));
        if (ab.durabilitySaver > 0) tooltip.add(Component.translatable("tooltip.berlords_food_system.ability.durability_saver",
                Math.round(ab.durabilitySaver * 100)).withStyle(ChatFormatting.AQUA));
    }

    private TooltipHandler() {}
}
