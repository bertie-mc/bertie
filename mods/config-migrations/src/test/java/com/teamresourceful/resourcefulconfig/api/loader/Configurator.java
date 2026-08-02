package com.teamresourceful.resourcefulconfig.api.loader;

import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig;
import java.util.function.Consumer;

/** Preserves both initial load-to-save call sites from Resourceful Config 3.0.11. */
public final class Configurator {
    private final String modid;

    public Configurator(String modid) {
        this.modid = modid;
    }

    public void register(Class<?> configClass, Consumer<?> patchHandler) {
        loadConfigClass(configClass, patchHandler);
    }

    public void register(ResourcefulConfig config, Consumer<?> patchHandler) {
        config.load(patchHandler);
        config.save();
    }

    private ResourcefulConfig loadConfigClass(Class<?> configClass, Consumer<?> patchHandler) {
        try {
            ResourcefulConfig config =
                    (ResourcefulConfig) configClass.getDeclaredConstructor().newInstance();
            config.load(patchHandler);
            config.save();
            return config;
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }
}
