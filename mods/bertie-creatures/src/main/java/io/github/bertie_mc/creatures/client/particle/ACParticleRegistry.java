package io.github.bertie_mc.creatures.client.particle;

import io.github.bertie_mc.creatures.BertieCreatures;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ACParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> DEF_REG =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, BertieCreatures.MODID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WATER_TREMOR =
            DEF_REG.register("water_tremor", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DINOSAUR_TRANSFORMATION_AMBER =
            DEF_REG.register("dinosaur_transformation_amber", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DINOSAUR_TRANSFORMATION_TECTONIC =
            DEF_REG.register("dinosaur_transformation_tectonic", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STUN_STAR =
            DEF_REG.register("stun_star", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TEPHRA =
            DEF_REG.register("tephra", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TEPHRA_SMALL =
            DEF_REG.register("tephra_small", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TEPHRA_FLAME =
            DEF_REG.register("tephra_flame", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LUXTRUCTOSAURUS_SPIT =
            DEF_REG.register("luxtructosaurus_spit", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LUXTRUCTOSAURUS_ASH =
            DEF_REG.register("luxtructosaurus_ash", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HAPPINESS =
            DEF_REG.register("happiness", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RED_VENT_SMOKE =
            DEF_REG.register("red_vent_smoke", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MUSHROOM_CLOUD =
            DEF_REG.register("mushroom_cloud", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MUSHROOM_CLOUD_SMOKE =
            DEF_REG.register("mushroom_cloud_smoke", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MUSHROOM_CLOUD_EXPLOSION =
            DEF_REG.register("mushroom_cloud_explosion", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DEEP_ONE_MAGIC =
            DEF_REG.register("deep_one_magic", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WATER_FOAM =
            DEF_REG.register("water_foam", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BIG_SPLASH =
            DEF_REG.register("big_splash", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BIG_SPLASH_EFFECT =
            DEF_REG.register("big_splash_effect", () -> new SimpleParticleType(false));
}
