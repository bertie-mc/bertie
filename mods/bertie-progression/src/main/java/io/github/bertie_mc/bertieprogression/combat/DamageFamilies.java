package io.github.bertie_mc.bertieprogression.combat;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.common.Tags;

public final class DamageFamilies {
    private static final ClassValue<Boolean> SPELL_SOURCE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                if (current.getName().equals("io.redspace.ironsspellbooks.damage.SpellDamageSource")) {
                    return true;
                }
            }
            return false;
        }
    };

    private DamageFamilies() {}

    public static DamageFamily of(DamageSource source) {
        if (source instanceof RoutedDamage routed && routed.bertie$getFamily() != null) {
            return routed.bertie$getFamily();
        }
        if (source.getClass()
                .getName()
                .equals("com.sammy.malum.common.geas.pact.eldritch.ArcanaphageGeas$ArcanaphageDamageSource"))
            return DamageFamily.ENERGY;
        Holder<DamageType> type = source.typeHolder();
        if (type.unwrapKey()
                        .map(key -> key.location().getNamespace().equals("fdbosses"))
                        .orElse(false)
                || source.getEntity() != null
                        && BuiltInRegistries.ENTITY_TYPE
                                .getKey(source.getEntity().getType())
                                .getNamespace()
                                .equals("fdbosses")) {
            return DamageFamily.PURE;
        }
        if (type.is(CombatTags.PURE)) {
            return DamageFamily.PURE;
        }
        if (SPELL_SOURCE.get(source.getClass()) || type.is(CombatTags.MAGIC) || MagicOrigins.isSpellDamage(source)) {
            return DamageFamily.MAGIC;
        }
        if (type.is(CombatTags.ENERGY)) {
            return DamageFamily.ENERGY;
        }
        if (type.is(CombatTags.PHYSICAL)) {
            return DamageFamily.PHYSICAL;
        }
        if (type.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return DamageFamily.PURE;
        }
        if (type.is(DamageTypeTags.IS_EXPLOSION)) {
            return DamageFamily.PHYSICAL;
        }
        return type.is(Tags.DamageTypes.IS_MAGIC)
                        || type.is(DamageTypeTags.IS_FIRE)
                        || type.is(DamageTypeTags.BYPASSES_ARMOR)
                ? DamageFamily.ENERGY
                : DamageFamily.PHYSICAL;
    }

    public static boolean isPure(DamageSource source) {
        return of(source) == DamageFamily.PURE;
    }

    public static boolean isEnergy(DamageSource source) {
        return of(source) == DamageFamily.ENERGY;
    }

    public static boolean isDot(DamageSource source) {
        return source.typeHolder().is(CombatTags.DOT)
                || source.getDirectEntity() != null
                        && source.getDirectEntity().getType().is(CombatTags.DOT_ENTITIES);
    }

    /** Null delegates an unrelated tag to the original source implementation. */
    public static Boolean matches(DamageSource source, TagKey<DamageType> tag) {
        ResourceLocation key = tag.location();
        String name = key.toString();
        DamageFamily family;
        switch (name) {
            case "minecraft:bypasses_shield" -> {
                return isPure(source) ? true : null;
            }
            case "minecraft:damages_helmet" -> {
                return isPure(source) ? false : null;
            }
            case "minecraft:bypasses_cooldown" -> {
                return source.typeHolder().is(CombatTags.SETTLED_PAYMENT) ? true : null;
            }
            case "enigmaticlegacyplus:spellstone/forgotten_ice/resistant_to" -> {
                family = of(source);
                return family == DamageFamily.ENERGY
                        || family == DamageFamily.PHYSICAL
                                && source.typeHolder().is(DamageTypeTags.IS_PROJECTILE);
            }
            case "additionalentityattributes:is_magic" -> {
                // Its old early subtraction is replaced by the shared late Energy subtraction.
                return false;
            }
            case "mekanism:is_preventable_magic" -> {
                return of(source) == DamageFamily.ENERGY && source.typeHolder().is(CombatTags.POISON);
            }
            case "minecraft:bypasses_armor",
                    "minecraft:bypasses_enchantments",
                    "minecraft:bypasses_effects",
                    "minecraft:bypasses_resistance" -> {
                // L2's explicit attack variants keep their own exceptions until the L2 pass.
                if (source.typeHolder()
                        .unwrapKey()
                        .map(k -> k.location().getNamespace().equals("l2damagetracker"))
                        .orElse(false)) {
                    return null;
                }
                family = of(source);
                return name.equals("minecraft:bypasses_armor") ? !family.usesArmor() : !family.usesProtection();
            }
            case "neoforge:is_magic", "c:is_magic", "minecraft:witch_resistant_to" -> {
                return isEnergy(source);
            }
            case "neoforge:is_physical", "c:is_physical" -> {
                return of(source) == DamageFamily.PHYSICAL;
            }
            case "minecraft:is_fire",
                    "minecraft:is_explosion",
                    "minecraft:is_projectile",
                    "minecraft:is_freezing",
                    "minecraft:is_lightning",
                    "minecraft:is_fall",
                    "neoforge:is_poison",
                    "c:is_poison" -> {
                family = of(source);
                if (family == DamageFamily.MAGIC || family == DamageFamily.PURE) {
                    return false;
                }
                if (name.equals("minecraft:is_explosion") && family != DamageFamily.PHYSICAL) return false;
                if (source instanceof RoutedDamage routed && routed.bertie$hasProperties()) {
                    int bit = DamageRoutes.propertyBit(name);
                    if (bit != 0) return (routed.bertie$properties() & bit) != 0;
                }
                if (name.equals("minecraft:is_fire") && source.typeHolder().is(CombatTags.FIRE)) {
                    return true;
                }
                if (name.equals("minecraft:is_projectile")
                        && source.getDirectEntity() instanceof Projectile projectile
                        && !projectile.getType().is(CombatTags.DOT_ENTITIES)
                        && !source.typeHolder().is(DamageTypeTags.IS_EXPLOSION)) return true;
                if (name.equals("minecraft:is_explosion")
                        && (source.typeHolder().is(CombatTags.EXPLOSION)
                                || source instanceof RoutedDamage routed && routed.bertie$isExplosion())) {
                    return true;
                }
                return null;
            }
            default -> {
                return null;
            }
        }
    }

    public static DamageSource explosion(DamageSource source) {
        DamageFamily family = of(source);
        if (family == DamageFamily.PURE || family == DamageFamily.MAGIC && SPELL_SOURCE.get(source.getClass())) {
            return source;
        }
        DamageSource copy = new DamageSource(
                source.typeHolder(), source.getDirectEntity(), source.getEntity(), source.getSourcePosition());
        RoutedDamage routed = (RoutedDamage) copy;
        routed.bertie$setFamily(family == DamageFamily.MAGIC ? DamageFamily.MAGIC : DamageFamily.PHYSICAL);
        if (family == DamageFamily.MAGIC) routed.bertie$school(MagicOrigins.school(source));
        routed.bertie$setExplosion(true);
        return copy;
    }
}
