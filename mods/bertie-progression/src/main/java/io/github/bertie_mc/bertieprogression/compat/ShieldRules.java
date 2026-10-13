package io.github.bertie_mc.bertieprogression.compat;

import io.github.bertie_mc.bertieprogression.combat.DamageRoutes;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class ShieldRules {
    public static final ResourceLocation DEEP = ResourceLocation.parse("alexsmobs:shield_of_the_deep");

    private ShieldRules() {}

    public static void components(ModifyDefaultComponentsEvent event) {
        stats(event, "block_factorys_bosses:enhanced_shield", 2, null, null, null);
        stats(event, "mythsandlegends:stony_shield", 4, 2.0, null, 880);
        stats(event, "block_factorys_bosses:dragon_guard_shield", 4, null, 0.05, null);
        stats(event, DEEP.toString(), 4, null, null, null);
        stats(event, "piglinproliferation:buckler", null, null, 0.10, null);
    }

    private static void stats(
            ModifyDefaultComponentsEvent event,
            String id,
            Integer armor,
            Double toughness,
            Double speed,
            Integer durability) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (item == Items.AIR) return;
        var builder = ItemAttributeModifiers.builder();
        var previous = item.components().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        for (var entry : previous.modifiers()) {
            if ((armor != null && entry.attribute().equals(Attributes.ARMOR))
                    || (toughness != null && entry.attribute().equals(Attributes.ARMOR_TOUGHNESS))
                    || (speed != null && entry.attribute().equals(Attributes.MOVEMENT_SPEED))) continue;
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        if (armor != null) add(builder, Attributes.ARMOR, "shield_armor", armor, AttributeModifier.Operation.ADD_VALUE);
        if (toughness != null)
            add(
                    builder,
                    Attributes.ARMOR_TOUGHNESS,
                    "shield_toughness",
                    toughness,
                    AttributeModifier.Operation.ADD_VALUE);
        if (speed != null)
            add(
                    builder,
                    Attributes.MOVEMENT_SPEED,
                    "shield_speed",
                    speed,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        event.modify(item, components -> {
            components.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
            if (durability != null) components.set(DataComponents.MAX_DAMAGE, durability);
        });
    }

    private static void add(
            ItemAttributeModifiers.Builder builder,
            Holder<Attribute> attribute,
            String id,
            double value,
            AttributeModifier.Operation operation) {
        builder.add(
                attribute,
                new AttributeModifier(ResourceLocation.fromNamespaceAndPath("bertieprogression", id), value, operation),
                EquipmentSlotGroup.OFFHAND);
    }

    public static boolean breathing(LivingEntity entity) {
        return entity instanceof Player
                && entity.isUsingItem()
                && BuiltInRegistries.ITEM.getKey(entity.getUseItem().getItem()).equals(DEEP);
    }

    public static void breathe(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof LivingEntity entity && breathing(entity)) {
            entity.setAirSupply(Math.min(entity.getMaxAirSupply(), Math.max(0, entity.getAirSupply()) + 5));
        }
    }

    public static void aquaticReflect(LivingShieldBlockEvent event) {
        if (event.getEntity().level().isClientSide()
                || !event.getBlocked()
                || event.getBlockedDamage() <= 0
                || DamageRoutes.redirected(event.getDamageSource())
                || !BuiltInRegistries.ITEM
                        .getKey(event.getEntity().getUseItem().getItem())
                        .equals(DEEP)
                || !(event.getDamageSource().getEntity() instanceof LivingEntity attacker)
                || attacker == event.getEntity()
                || !attacker.isInWaterOrRain()) return;
        LivingEntity defender = event.getEntity();
        attacker.hurt(
                DamageRoutes.reflect(
                        event.getDamageSource(), defender.damageSources().thorns(defender), defender),
                event.getBlockedDamage() * 0.30f);
    }
}
