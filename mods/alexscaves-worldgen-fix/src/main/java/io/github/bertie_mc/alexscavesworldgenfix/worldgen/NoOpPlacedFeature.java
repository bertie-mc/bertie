package io.github.bertie_mc.alexscavesworldgenfix.worldgen;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * A placed feature that stands in for one the feature sorter cannot identify, and places nothing.
 *
 * <p>It carries no placement modifiers, so {@code placeWithBiomeCheck} reduces to a single call to
 * {@link Feature#NO_OP}, which reports failure without touching the level. The decoration loop then
 * moves on to the next index and, crucially, to the next generation step.
 *
 * <p>Held off the mixin so it is a normal class with a normal initialiser - a static field declared
 * in a mixin is merged into {@code ChunkGenerator} itself. Resolved lazily on first decoration,
 * long after {@code Feature.NO_OP} is registered.
 */
public final class NoOpPlacedFeature {

    private static final PlacedFeature INSTANCE = create();

    private NoOpPlacedFeature() {
    }

    public static PlacedFeature get() {
        return INSTANCE;
    }

    private static PlacedFeature create() {
        // Declared at the target type on purpose: handing Holder.direct a local typed
        // ConfiguredFeature<?, ?> captures the wildcards and no longer matches PlacedFeature.
        Holder<ConfiguredFeature<?, ?>> configured =
                Holder.direct(new ConfiguredFeature<>(Feature.NO_OP, NoneFeatureConfiguration.INSTANCE));
        return new PlacedFeature(configured, List.of());
    }
}
