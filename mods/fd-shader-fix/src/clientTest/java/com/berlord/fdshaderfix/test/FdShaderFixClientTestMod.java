package com.berlord.fdshaderfix.test;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Arrays;

@Mod(value = FdShaderFixClientTestMod.MOD_ID, dist = Dist.CLIENT)
public final class FdShaderFixClientTestMod {
    static final String MOD_ID = "fdshaderfixtest";
    private static final String TARGET =
            "com.finderfeed.fdlib.systems.post_shaders.FDPostShadersReloadableResourceListener";
    private static final Logger LOGGER = LogUtils.getLogger();

    public FdShaderFixClientTestMod(IEventBus modBus) {
        modBus.addListener(this::onLoadComplete);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            try {
                Class<?> listener = Class.forName(TARGET);
                boolean applied = Arrays.stream(listener.getDeclaredMethods())
                        .map(Method::getName)
                        .anyMatch(name -> name.contains("fdshaderfix$resilientInit"));
                if (!applied) {
                    throw new IllegalStateException("fdlib shader listener is missing the fix wrapper");
                }
                LOGGER.info("FD_SHADER_FIX_MIXIN_OK");
            } catch (ClassNotFoundException failure) {
                throw new IllegalStateException("fdlib shader listener is unavailable", failure);
            }
        });
    }
}
