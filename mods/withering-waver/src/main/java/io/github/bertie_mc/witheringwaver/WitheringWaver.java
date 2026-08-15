package io.github.bertie_mc.witheringwaver;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Adds the Withering Waver, a rare wither skeleton variant. */
@Mod(WitheringWaver.MOD_ID)
public class WitheringWaver {
    public static final String MOD_ID = "witheringwaver";

    public WitheringWaver(IEventBus modBus) {
        WwEntities.ENTITY_TYPES.register(modBus);
        WwItems.ITEMS.register(modBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
