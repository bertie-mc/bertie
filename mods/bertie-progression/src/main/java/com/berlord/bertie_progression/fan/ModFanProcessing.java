package com.berlord.bertie_progression.fan;

import com.berlord.bertie_progression.BertieProgression;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers our fan-processing types into Create's own registry. */
public final class ModFanProcessing {

    public static final DeferredRegister<FanProcessingType> TYPES =
            DeferredRegister.create(CreateRegistries.FAN_PROCESSING_TYPE, BertieProgression.MODID);

    public static final DeferredHolder<FanProcessingType, OminousFanProcessingType> OMINOUS =
            TYPES.register("ominous", OminousFanProcessingType::new);

    private ModFanProcessing() {
    }
}
