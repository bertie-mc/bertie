package io.github.bertie_mc.bertieprogression.compat;

import auviotre.enigmatic.legacy.registries.EnigmaticComponents;
import auviotre.enigmatic.legacy.registries.EnigmaticItems;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

public final class CursedTomeRules {
    private CursedTomeRules() {}

    @SubscribeEvent
    public static void markCursed(ModifyDefaultComponentsEvent event) {
        if (ModList.get().isLoaded("enigmaticlegacyplus")) {
            event.modify(
                    EnigmaticItems.ENCHANTMENT_TRANSPOSER.get(),
                    components -> components.set(EnigmaticComponents.CURSED.get(), true));
        }
    }
}
