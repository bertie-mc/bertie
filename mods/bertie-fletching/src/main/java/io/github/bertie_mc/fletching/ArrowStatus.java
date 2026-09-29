package io.github.bertie_mc.fletching;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ArrowStatus extends MobEffect {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, BertieFletching.ID);
    public static final DeferredHolder<MobEffect, ArrowStatus> FROZEN =
            EFFECTS.register("frozen", () -> new ArrowStatus(0x9be2f2));
    public static final DeferredHolder<MobEffect, ArrowStatus> FRIGHTENED =
            EFFECTS.register("frightened", () -> new ArrowStatus(0x604583));
    public static final DeferredHolder<MobEffect, ArrowStatus> VULNERABLE =
            EFFECTS.register("projectile_vulnerability", () -> new ArrowStatus(0xdf7338));

    private ArrowStatus(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }
}
