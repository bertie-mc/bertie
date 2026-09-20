package io.github.bertie_mc.bertieprogression;

import io.github.bertie_mc.bertieprogression.fan.ModFanProcessing;
import io.github.bertie_mc.bertieprogression.forge.ForgeBedHandler;
import io.github.bertie_mc.bertieprogression.forge.PedestalFormationHandler;
import io.github.bertie_mc.bertieprogression.gate.CraftingGateHandler;
import io.github.bertie_mc.bertieprogression.gate.PlankSplitHandler;
import io.github.bertie_mc.bertieprogression.hooks.HookIntegration;
import io.github.bertie_mc.bertieprogression.pocket.PocketTravelHandler;
import io.github.bertie_mc.bertieprogression.recipe.ModRecipes;
import io.github.bertie_mc.bertieprogression.torch.MagnumTorchHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(BertieProgression.MODID)
public final class BertieProgression {
    public static final String MODID = "bertieprogression";

    public BertieProgression(IEventBus modBus) {
        ModItems.ITEMS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.DATA_COMPONENTS.register(modBus);
        ModItems.TABS.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        HazenUnreleased.ARMOR_MATERIALS.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModRecipes.TYPES.register(modBus);
        // Create is an optional integration; do not resolve its registry classes otherwise.
        if (ModList.get().isLoaded("create")) {
            ModFanProcessing.TYPES.register(modBus);
        }
        // Same rule for Forbidden Arcanus: the ritual result type lives in ITS registry, so naming
        // the class at all without the mod present would fail to link.
        if (ModList.get().isLoaded("forbidden_arcanus")) {
            io.github.bertie_mc.bertieprogression.backpack.ModRitualResults.TYPES.register(modBus);
        }
        // Four new hook variants live in this mod's namespace and four built-ins get rebalanced;
        // guarded because the whole class references hooked's Kotlin types directly.
        if (ModList.get().isLoaded("hooked")) {
            HookIntegration.init(modBus);
        }
        if (ModList.get().isLoaded("pocket_dimension")) {
            NeoForge.EVENT_BUS.register(PocketTravelHandler.class);
        }
        // BuildCreativeModeTabContentsEvent is a MOD-bus event, not a game-bus one.
        modBus.register(RemovedItems.class);
        modBus.register(TabAnchors.class);
        modBus.register(EldritchTabGaps.class);
        // ModifyDefaultComponentsEvent is a mod-bus event as well.
        modBus.register(TrinketConversions.class);
        // RegisterEvent and BuildCreativeModeTabContentsEvent, both mod-bus.
        modBus.register(HazenUnreleased.class);
        // RegisterCapabilitiesEvent is a mod-bus event too. The class guards on create + curios.
        modBus.register(ExtendoGripCurio.class);

        NeoForge.EVENT_BUS.register(CraftingGateHandler.class);
        NeoForge.EVENT_BUS.register(PlankSplitHandler.class);
        NeoForge.EVENT_BUS.register(MagnumTorchHandler.class);
        NeoForge.EVENT_BUS.register(ForgeBedHandler.class);
        NeoForge.EVENT_BUS.register(PedestalFormationHandler.class);
        NeoForge.EVENT_BUS.register(AllayCorruptionHandler.class);
        NeoForge.EVENT_BUS.register(NetherGateHandler.class);
        NeoForge.EVENT_BUS.register(CrushingEssenceHandler.class);
        NeoForge.EVENT_BUS.register(EndVeilBrewingHandler.class);
        NeoForge.EVENT_BUS.register(MagicMirrorCooldownHandler.class);
        NeoForge.EVENT_BUS.register(CapeSlotHandler.class);
        NeoForge.EVENT_BUS.register(SlotAudit.class);
        NeoForge.EVENT_BUS.register(io.github.bertie_mc.bertieprogression.altar.AltarTooltipHandler.class);
    }
}
