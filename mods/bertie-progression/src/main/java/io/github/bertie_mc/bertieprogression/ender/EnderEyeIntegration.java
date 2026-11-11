package io.github.bertie_mc.bertieprogression.ender;

import com.mojang.serialization.MapCodec;
import io.github.bertie_mc.bertieprogression.BertieProgression;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class EnderEyeIntegration {
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, BertieProgression.MODID);

    static {
        LOOT.register("endrem_eye_sources", () -> EyeLootModifier.CODEC);
    }

    public static void register(IEventBus bus) {
        LOOT.register(bus);
        NeoForge.EVENT_BUS.register(EyeAcquisitions.class);
    }

    private EnderEyeIntegration() {}
}
