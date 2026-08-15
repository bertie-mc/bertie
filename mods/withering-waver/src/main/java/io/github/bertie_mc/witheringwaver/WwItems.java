package io.github.bertie_mc.witheringwaver;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class WwItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WitheringWaver.MOD_ID);

    public static final DeferredItem<DeferredSpawnEggItem> WITHERING_WAVER_SPAWN_EGG = ITEMS.register(
            "withering_waver_spawn_egg",
            () -> new DeferredSpawnEggItem(WwEntities.WITHERING_WAVER, 0x141216, 0xC9A227, new Item.Properties()));
}
