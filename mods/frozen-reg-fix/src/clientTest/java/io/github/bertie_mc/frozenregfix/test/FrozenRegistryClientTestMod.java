package io.github.bertie_mc.frozenregfix.test;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;

import java.util.List;

@Mod(value = FrozenRegistryClientTestMod.MOD_ID, dist = Dist.CLIENT)
public final class FrozenRegistryClientTestMod {
    static final String MOD_ID = "frozenregfixtest";
    private static final String SUCCESS_MARKER = "FROZEN_REGISTRY_ARMOR_MATERIALS_OK";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<String> MATERIALS = List.of(
            "bone",
            "wither",
            "warrior",
            "heavy",
            "robe",
            "slime",
            "divine",
            "prismarine",
            "wooden",
            "steampunk"
    );

    public FrozenRegistryClientTestMod(IEventBus modBus) {
        modBus.addListener(this::onLoadComplete);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            forceLazyItemsClass();
            for (String path : MATERIALS) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("immersive_armors", path);
                if (!BuiltInRegistries.ARMOR_MATERIAL.containsKey(id)) {
                    throw new IllegalStateException("Immersive Armors material is not registered: " + id);
                }
            }
            LOGGER.info(SUCCESS_MARKER);
        });
    }

    private static void forceLazyItemsClass() {
        try {
            Class.forName("immersive_armors.Items", true, FrozenRegistryClientTestMod.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Immersive Armors Items class is missing", exception);
        }
    }
}
