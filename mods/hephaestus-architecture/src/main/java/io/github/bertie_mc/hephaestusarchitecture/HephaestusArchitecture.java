package io.github.bertie_mc.hephaestusarchitecture;

import io.github.bertie_mc.hephaestusarchitecture.item.ForgeStructurePlacerItem;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(HephaestusArchitecture.MOD_ID)
public final class HephaestusArchitecture {

    public static final String MOD_ID = "hephaestusarchitecture";
    public static final Logger LOGGER = LoggerFactory.getLogger("HephaestusArchitecture");

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    /** One creative-only placer per forge tier; each builds its tier's complete layout. */
    public static final List<DeferredItem<ForgeStructurePlacerItem>> STRUCTURE_PLACERS = IntStream.rangeClosed(1, 5)
            .mapToObj(tier -> ITEMS.register(
                    "forge_tier_" + tier + "_structure_placer",
                    () -> new ForgeStructurePlacerItem(
                            tier, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))))
            .toList();

    private static final ResourceKey<CreativeModeTab> FORBIDDEN_ARCANUS_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("forbidden_arcanus", "main"));

    public HephaestusArchitecture(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(HephaestusArchitecture::addToForbiddenArcanusTab);
        LOGGER.info("Tiered Hephaestus Forge structure support enabled");
    }

    private static void addToForbiddenArcanusTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != FORBIDDEN_ARCANUS_TAB) {
            return;
        }
        for (DeferredItem<ForgeStructurePlacerItem> placer : STRUCTURE_PLACERS) {
            event.accept(placer.get());
        }
    }
}
