package com.berlord.hephaestusarchitecture.test;

import com.berlord.hephaestusarchitecture.mixin.HephaestusForgeBlockEntityAccessor;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Mod(value = HephaestusArchitectureClientTestMod.MOD_ID, dist = Dist.CLIENT)
public final class HephaestusArchitectureClientTestMod {
    static final String MOD_ID = "hephaestusarchitecturetest";
    private static final Logger LOGGER = LogUtils.getLogger();

    public HephaestusArchitectureClientTestMod(IEventBus modBus) {
        modBus.addListener(this::onLoadComplete);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            try {
                Class<?> forge = Class.forName(
                        "com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeBlockEntity");
                if (!HephaestusForgeBlockEntityAccessor.class.isAssignableFrom(forge)) {
                    throw new IllegalStateException("F&A forge does not implement the accessor mixin");
                }
                assertMethods(Class.forName("com.stal111.forbidden_arcanus.common.block.HephaestusForgeBlock"),
                        "hephaestusarchitecture$validateTierLayout",
                        "hephaestusarchitecture$rejectRitualOnInvalidLayout",
                        "hephaestusarchitecture$rejectMenuOnInvalidLayout");
                assertMethods(Class.forName(
                                "com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.Ritual"),
                        "hephaestusarchitecture$expandInputLimit");
                assertMethods(Class.forName(
                                "com.stal111.forbidden_arcanus.common.block.pedestal.effect.UpdateForgeIngredientsEffect"),
                        "hephaestusarchitecture$routeByTierLayout");
                assertMethods(Class.forName(
                                "com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.UpgradeTierResult"),
                        "hephaestusarchitecture$validateUpgradedLayout");
                LOGGER.info("HEPHAESTUS_ARCHITECTURE_MIXINS_OK");
            } catch (ClassNotFoundException failure) {
                throw new IllegalStateException("Forbidden & Arcanus forge classes are unavailable", failure);
            }
        });
    }

    private static void assertMethods(Class<?> target, String... fragments) {
        Set<String> methods = Arrays.stream(target.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
        for (String fragment : fragments) {
            if (methods.stream().noneMatch(name -> name.contains(fragment))) {
                throw new IllegalStateException(target.getName() + " is missing " + fragment);
            }
        }
    }
}
