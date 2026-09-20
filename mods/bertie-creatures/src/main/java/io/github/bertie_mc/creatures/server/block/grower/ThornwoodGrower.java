package io.github.bertie_mc.creatures.server.block.grower;

import io.github.bertie_mc.creatures.BertieCreatures;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class ThornwoodGrower {

    public static final ResourceKey<ConfiguredFeature<?, ?>> THORNWOOD_TREE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(BertieCreatures.MODID, "thornwood_tree"));

    public static final TreeGrower GROWER =
            new TreeGrower("thornwood", Optional.empty(), Optional.of(THORNWOOD_TREE), Optional.empty());
}
