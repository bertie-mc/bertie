package io.github.bertie_mc.bertieprogression.worldgen;

import io.github.bertie_mc.bertieprogression.BertieProgression;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ProgressionFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, BertieProgression.MODID);

    static {
        FEATURES.register("solar_pool_ore", SolarPoolOreFeature::new);
        FEATURES.register("rising_tungsten_ore", RisingTungstenOreFeature::new);
    }

    private ProgressionFeatures() {}
}
