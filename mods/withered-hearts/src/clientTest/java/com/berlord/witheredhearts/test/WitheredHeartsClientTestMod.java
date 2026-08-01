package com.berlord.witheredhearts.test;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.Gui;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Mod(value = WitheredHeartsClientTestMod.MOD_ID, dist = Dist.CLIENT)
public final class WitheredHeartsClientTestMod {
    static final String MOD_ID = "witheredheartstest";
    private static final String SUCCESS_MARKER = "WITHERED_HEARTS_GUI_MIXIN_OK";
    private static final Logger LOGGER = LogUtils.getLogger();

    public WitheredHeartsClientTestMod(IEventBus modBus) {
        modBus.addListener(this::onLoadComplete);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            Set<String> methods = Arrays.stream(Gui.class.getDeclaredMethods())
                    .map(Method::getName)
                    .collect(Collectors.toSet());
            assertMixinMethod(methods, "witheredhearts$wrapForPlayer");
            assertMixinMethod(methods, "witheredhearts$wrapRenderHeart");
            LOGGER.info(SUCCESS_MARKER);
        });
    }

    private static void assertMixinMethod(Set<String> methods, String fragment) {
        if (methods.stream().noneMatch(name -> name.contains(fragment))) {
            throw new IllegalStateException("Gui is missing " + fragment + ": " + methods);
        }
    }
}
