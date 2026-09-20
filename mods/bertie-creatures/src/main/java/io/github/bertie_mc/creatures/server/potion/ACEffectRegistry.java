package io.github.bertie_mc.creatures.server.potion;

import io.github.bertie_mc.creatures.BertieCreatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ACEffectRegistry {
    public static final DeferredRegister<MobEffect> DEF_REG =
            DeferredRegister.create(Registries.MOB_EFFECT, BertieCreatures.MODID);
    public static final DeferredHolder<MobEffect, MobEffect> STUNNED =
            DEF_REG.register("stunned", () -> new StunnedEffect());
    public static final DeferredHolder<MobEffect, MobEffect> RAGE = DEF_REG.register("rage", () -> new RageEffect());
    public static final DeferredHolder<MobEffect, MobEffect> IRRADIATED =
            DEF_REG.register("irradiated", () -> new IrradiatedEffect());
    public static final DeferredHolder<MobEffect, MobEffect> BUBBLED =
            DEF_REG.register("bubbled", () -> new BubbledEffect());
}
