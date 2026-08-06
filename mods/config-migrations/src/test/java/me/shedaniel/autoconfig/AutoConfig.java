package me.shedaniel.autoconfig;

import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.ConfigSerializer;

/** Preserves the real registration call shape so the optional mixin target is exercised. */
public final class AutoConfig {
    private AutoConfig() {}

    public static <T extends ConfigData> ConfigHolder<T> register(
            Class<T> configClass, ConfigSerializer.Factory<T> factory) {
        Config definition = configClass.getAnnotation(Config.class);
        factory.create(definition, configClass);
        return null;
    }
}
