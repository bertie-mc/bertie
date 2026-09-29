package io.github.bertie_mc.bertieprogression.combat;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class ArmorDefenses {
    private static final Set<ResourceLocation> EFFECT_PENALTIES = ConcurrentHashMap.newKeySet();
    private static final Map<Holder<Attribute>, Set<ResourceLocation>> RETIRED_MODIFIERS = new ConcurrentHashMap<>();

    private ArmorDefenses() {}

    public static void registerPenalty(ResourceLocation id) {
        EFFECT_PENALTIES.add(id);
    }

    public static void retire(Holder<Attribute> attribute, ResourceLocation id) {
        RETIRED_MODIFIERS
                .computeIfAbsent(attribute, ignored -> ConcurrentHashMap.newKeySet())
                .add(id);
    }

    @SubscribeEvent
    public static void removeSavedPenalties(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity entity) {
            RETIRED_MODIFIERS.forEach((attribute, ids) -> {
                AttributeInstance instance = entity.getAttribute(attribute);
                if (instance != null) ids.forEach(instance::removeModifier);
            });
        }
    }

    public static double effectiveArmor(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attributes.ARMOR);
        if (attribute == null) {
            return 0.0;
        }
        double counter = CombatMath.toughnessCounter(entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        if (counter == 0 || attribute.getModifiers().stream().noneMatch(ArmorDefenses::isPenalty)) {
            return attribute.getValue();
        }
        double base = attribute.getBaseValue();
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                base += amount(modifier, counter);
            }
        }
        double result = base;
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                result += base * amount(modifier, counter);
            }
        }
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                result *= 1.0 + amount(modifier, counter);
            }
        }
        return attribute.getAttribute().value().sanitizeValue(result);
    }

    private static boolean isPenalty(AttributeModifier modifier) {
        return modifier.amount() < 0 && EFFECT_PENALTIES.contains(modifier.id());
    }

    private static double amount(AttributeModifier modifier, double counter) {
        return modifier.amount() * (isPenalty(modifier) ? 1.0 - counter : 1.0);
    }
}
