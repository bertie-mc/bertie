package io.github.bertie_mc.bertieprogression.combat;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

public final class CombatRegistry {
    private static final Map<String, Optional<Holder.Reference<Attribute>>> ATTRIBUTES = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Holder.Reference<MobEffect>>> EFFECTS = new ConcurrentHashMap<>();

    private CombatRegistry() {}

    public static AttributeInstance attribute(LivingEntity entity, String id) {
        return ATTRIBUTES
                .computeIfAbsent(id, key -> BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(key)))
                .map(entity::getAttribute)
                .orElse(null);
    }

    public static double value(LivingEntity entity, String id, double fallback) {
        AttributeInstance instance = attribute(entity, id);
        return instance == null ? fallback : instance.getValue();
    }

    public static Optional<Holder.Reference<MobEffect>> effect(String id) {
        return EFFECTS.computeIfAbsent(id, key -> BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(key)));
    }

    public static int effectLevel(LivingEntity entity, String id) {
        return effect(id)
                .map(entity::getEffect)
                .map(instance -> instance.getAmplifier() + 1)
                .orElse(0);
    }
}
