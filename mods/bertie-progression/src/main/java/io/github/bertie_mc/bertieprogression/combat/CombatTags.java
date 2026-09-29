package io.github.bertie_mc.bertieprogression.combat;

import io.github.bertie_mc.bertieprogression.BertieProgression;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;

public final class CombatTags {
    public static final TagKey<DamageType> PHYSICAL = tag("physical");
    public static final TagKey<DamageType> MAGIC = tag("magic");
    public static final TagKey<DamageType> ENERGY = tag("energy");
    public static final TagKey<DamageType> PURE = tag("pure");
    public static final TagKey<DamageType> FIRE = tag("fire");
    public static final TagKey<DamageType> EXPLOSION = tag("explosion");
    public static final TagKey<DamageType> DOT = tag("damage_over_time");
    public static final TagKey<DamageType> POISON = tag("poison");
    public static final TagKey<DamageType> CONTACT_FIRE = tag("contact_fire");
    public static final TagKey<DamageType> SETTLED_PAYMENT = tag("settled_payment");
    public static final TagKey<EntityType<?>> DOT_ENTITIES = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, "combat/damage_over_time"));

    private CombatTags() {}

    private static TagKey<DamageType> tag(String name) {
        return TagKey.create(
                Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, "combat/" + name));
    }
}
