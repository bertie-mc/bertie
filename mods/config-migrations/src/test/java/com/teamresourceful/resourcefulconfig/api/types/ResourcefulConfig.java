package com.teamresourceful.resourcefulconfig.api.types;

import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigEntry;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

/** Minimal public shape used by Resourceful Config's Configurator and writer. */
public interface ResourcefulConfig {
    default int version() {
        return 1;
    }

    String id();

    LinkedHashMap<String, ResourcefulConfigEntry> entries();

    LinkedHashMap<String, ResourcefulConfig> categories();

    void save();

    default void load(Consumer<?> patchHandler) {}
}
