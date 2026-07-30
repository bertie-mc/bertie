package com.berlord.hephaestusarchitecture.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.stal111.forbidden_arcanus.common.block.entity.forge.circle.MagicCircleType;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.Ritual;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.RitualInput;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.RitualRequirements;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.RitualResult;
import net.minecraft.util.ExtraCodecs;

public final class ExpandedRitualCodecs {

    public static final int MAX_INPUT_GROUPS = 64;

    private ExpandedRitualCodecs() {
    }

    public static Codec<Ritual> directCodec() {
        return RecordCodecBuilder.create(instance -> instance.group(
                RitualInput.CODEC.listOf(1, MAX_INPUT_GROUPS)
                        .fieldOf("inputs")
                        .forGetter(Ritual::inputs),
                net.minecraft.world.item.crafting.Ingredient.CODEC_NONEMPTY
                        .fieldOf("main_ingredient")
                        .forGetter(Ritual::mainIngredient),
                RitualResult.DIRECT_CODEC
                        .fieldOf("result")
                        .forGetter(Ritual::result),
                RitualRequirements.CODEC.forGetter(Ritual::requirements),
                MagicCircleType.CODEC
                        .fieldOf("magic_circle")
                        .forGetter(Ritual::magicCircleType),
                ExtraCodecs.POSITIVE_INT
                        .optionalFieldOf("duration", Ritual.DEFAULT_DURATION)
                        .forGetter(Ritual::duration)
        ).apply(instance, Ritual::new));
    }
}
