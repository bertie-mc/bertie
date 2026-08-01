package com.berlord.hephaestusarchitecture.mixin;

import com.berlord.hephaestusarchitecture.ritual.ExpandedRitualCodecs;
import com.mojang.serialization.Codec;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.Ritual;
import com.stal111.forbidden_arcanus.core.registry.FARegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryFileCodec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Ritual.class)
public abstract class RitualCodecMixin {

    @Shadow(remap = false)
    @Final
    @Mutable
    public static Codec<Ritual> DIRECT_CODEC;

    @Shadow(remap = false)
    @Final
    @Mutable
    public static Codec<Holder<Ritual>> CODEC;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void hephaestusarchitecture$expandInputLimit(CallbackInfo ci) {
        DIRECT_CODEC = ExpandedRitualCodecs.directCodec();
        CODEC = RegistryFileCodec.create(FARegistries.RITUAL, DIRECT_CODEC);
    }
}
