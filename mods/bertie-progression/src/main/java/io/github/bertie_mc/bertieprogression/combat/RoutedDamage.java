package io.github.bertie_mc.bertieprogression.combat;

import net.minecraft.resources.ResourceLocation;

/** Per-source information for attacks whose damage type is reused by unrelated mechanics. */
public interface RoutedDamage {
    DamageFamily bertie$getFamily();

    void bertie$setFamily(DamageFamily family);

    boolean bertie$isExplosion();

    void bertie$setExplosion(boolean explosion);

    boolean bertie$isTransferred();

    void bertie$setTransferred(boolean transferred);

    ResourceLocation bertie$school();

    void bertie$school(ResourceLocation school);

    int bertie$properties();

    boolean bertie$hasProperties();

    void bertie$properties(int properties);
}
