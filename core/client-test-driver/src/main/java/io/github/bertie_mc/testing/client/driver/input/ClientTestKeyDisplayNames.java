package io.github.bertie_mc.testing.client.driver.input;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;

/** Initializes lazy key labels while GLFW is being called from the render thread. */
public final class ClientTestKeyDisplayNames {
    private ClientTestKeyDisplayNames() {}

    public static void preload(Options options) {
        preload(options.keyMappings);
    }

    static void preload(KeyMapping[] mappings) {
        for (KeyMapping mapping : mappings) {
            mapping.getTranslatedKeyMessage();
        }
    }
}
