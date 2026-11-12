package io.github.bertie_mc.bertieprogression.compat;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.upgrade.UpgradeState;
import com.refinedmods.refinedstorage.common.upgrade.FortuneUpgradeItem;
import com.refinedmods.refinedstorage.common.upgrade.UpgradeDestinations;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RefinedFortune {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("refinedstorage");
    public static final Map<Integer, DeferredItem<FortuneUpgradeItem>> UPGRADES = new LinkedHashMap<>();

    static {
        for (int level = 4; level <= 8; level++) {
            final int rank = level;
            UPGRADES.put(
                    level,
                    ITEMS.register(
                            "fortune_" + level + "_upgrade",
                            () -> new FortuneUpgradeItem(RefinedStorageApi.INSTANCE.getUpgradeRegistry(), rank)));
        }
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(RefinedFortune::setup);
        bus.addListener(RefinedFortune::creative);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> UPGRADES.values()
                .forEach(item -> RefinedStorageApi.INSTANCE
                        .getUpgradeRegistry()
                        .forDestination(UpgradeDestinations.DESTRUCTOR)
                        .add(item.get(), 1)));
    }

    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().location().getNamespace().equals("refinedstorage")) {
            ItemStack pivot = event.getParentEntries().stream()
                    .filter(stack -> BuiltInRegistries.ITEM
                            .getKey(stack.getItem())
                            .toString()
                            .equals("refinedstorage:fortune_3_upgrade"))
                    .findFirst()
                    .orElse(ItemStack.EMPTY);
            if (pivot.isEmpty()) return;
            for (var item : UPGRADES.values()) {
                ItemStack next = new ItemStack(item.get());
                event.insertAfter(pivot, next, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                pivot = next;
            }
        }
    }

    public static boolean enchantTool(ServerLevel level, UpgradeState upgrades, ItemStack tool) {
        if (upgrades.has(com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getSilkTouchUpgrade())) {
            return false;
        }
        for (int rank = 8; rank >= 4; rank--) {
            if (upgrades.has(UPGRADES.get(rank).get())) {
                tool.enchant(
                        level.registryAccess()
                                .lookupOrThrow(Registries.ENCHANTMENT)
                                .getOrThrow(Enchantments.FORTUNE),
                        rank);
                return true;
            }
        }
        return false;
    }

    private RefinedFortune() {}
}
