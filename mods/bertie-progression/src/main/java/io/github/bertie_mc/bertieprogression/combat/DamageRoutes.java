package io.github.bertie_mc.bertieprogression.combat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.Tags;

public final class DamageRoutes {
    public record Snapshot(DamageFamily family, int properties, ResourceLocation school) {}

    private static final TagKey<DamageType>[] PROPERTIES = properties();

    private DamageRoutes() {}

    @SuppressWarnings("unchecked")
    private static TagKey<DamageType>[] properties() {
        return new TagKey[] {
            DamageTypeTags.IS_FIRE,
            DamageTypeTags.IS_EXPLOSION,
            DamageTypeTags.IS_PROJECTILE,
            DamageTypeTags.IS_FREEZING,
            DamageTypeTags.IS_LIGHTNING,
            Tags.DamageTypes.IS_POISON,
            DamageTypeTags.IS_FALL
        };
    }

    public static int propertyBit(String tag) {
        return switch (tag) {
            case "minecraft:is_fire" -> 1;
            case "minecraft:is_explosion" -> 2;
            case "minecraft:is_projectile" -> 4;
            case "minecraft:is_freezing" -> 8;
            case "minecraft:is_lightning" -> 16;
            case "neoforge:is_poison", "c:is_poison" -> 32;
            case "minecraft:is_fall" -> 64;
            default -> 0;
        };
    }

    public static Snapshot snapshot(DamageSource source) {
        int properties = 0;
        for (int i = 0; i < PROPERTIES.length; i++) if (source.is(PROPERTIES[i])) properties |= 1 << i;
        ResourceLocation school = ModList.get().isLoaded("irons_spellbooks")
                ? SpellResistance.schoolOf(source)
                : MagicOrigins.school(source);
        return new Snapshot(DamageFamilies.of(source), properties, school);
    }

    public static boolean redirected(DamageSource source) {
        return source instanceof RoutedDamage routed && routed.bertie$isTransferred();
    }

    public static DamageSource transfer(DamageSource source) {
        DamageSource copy = new DamageSource(
                source.typeHolder(), source.getDirectEntity(), source.getEntity(), source.getSourcePosition());
        return describe(copy, snapshot(source));
    }

    public static DamageSource typed(Entity recipient, DamageSource original, String id, DamageFamily family) {
        var key = ResourceKey.create(
                Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("bertieprogression", id));
        var type = recipient
                .registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key);
        DamageSource changed =
                new DamageSource(type, original.getDirectEntity(), original.getEntity(), original.getSourcePosition());
        ((RoutedDamage) changed).bertie$setFamily(family);
        return changed;
    }

    public static DamageSource reflect(DamageSource original, DamageSource nativeCounter, Entity reflector) {
        return reflect(snapshot(original), nativeCounter, reflector);
    }

    public static DamageSource reflect(Snapshot original, DamageSource nativeCounter, Entity reflector) {
        DamageSource copy = new DamageSource(
                nativeCounter.typeHolder(), null, reflector, reflector == null ? null : reflector.position());
        return describe(copy, original);
    }

    private static DamageSource describe(DamageSource copy, Snapshot original) {
        RoutedDamage routed = (RoutedDamage) copy;
        routed.bertie$setFamily(original.family());
        routed.bertie$properties(original.properties());
        routed.bertie$school(original.school());
        routed.bertie$setTransferred(true);
        return copy;
    }
}
