package de.ambertation.wunderlib.utils;

import net.minecraft.resources.ResourceLocation;

public final class Version {
    private Version() {
    }

    public interface ModVersionProvider {
        String getNamespace();

        default ResourceLocation mk(String path) {
            return ResourceLocation.fromNamespaceAndPath(getNamespace(), path);
        }
    }
}
