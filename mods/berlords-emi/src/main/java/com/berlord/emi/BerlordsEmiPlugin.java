package com.berlord.emi;

import com.berlord.emi.anvilcraft.AnvilCraftEmiModule;
import com.berlord.emi.avaritiadelight.AvaritiaDelightEmiModule;
import com.berlord.emi.berriescherries.BerriesAndCherriesEmiModule;
import com.berlord.emi.betterarcheology.BetterArcheologyEmiModule;
import com.berlord.emi.cataclysm.CataclysmEmiModule;
import com.berlord.emi.cognition.CognitionEmiModule;
import com.berlord.emi.create.CreateEmiModule;
import com.berlord.emi.cuisinedelight.CuisineDelightEmiModule;
import com.berlord.emi.dungeonsdelight.DungeonsDelightEmiModule;
import com.berlord.emi.enderio.EnderIOEmiModule;
import com.berlord.emi.expandeddelight.ExpandedDelightEmiModule;
import com.berlord.emi.extradelight.ExtraDelightEmiModule;
import com.berlord.emi.farmerspizzeria.FarmersPizzeriaEmiModule;
import com.berlord.emi.forbiddenarcanus.ForbiddenArcanusEmiModule;
import com.berlord.emi.ironsspellbooks.IronsSpellbooksEmiModule;
import com.berlord.emi.l2complements.L2ComplementsEmiModule;
import com.berlord.emi.malum.MalumEmiModule;
import com.berlord.emi.slag.SlagEmiModule;
import com.berlord.emi.slavicdelight.SlavicDelightEmiModule;
import com.berlord.emi.stellaris.StellarisEmiModule;
import com.berlord.emi.terracurio.TerraCurioEmiModule;
import com.berlord.emi.twilightdelight.TwilightDelightEmiModule;
import com.berlord.emi.youkaisfeasts.YoukaisFeastsEmiModule;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for all of Berlord's native EMI machine integrations. EMI discovers this class by an
 * ASM scan for the {@link EmiEntrypoint} annotation — no service file or mods.toml entrypoint, and
 * the annotation must stay RuntimeInvisible (do NOT add {@code @Retention(RUNTIME)}).
 *
 * <p>Each mod's module is gated behind {@link ModList#isLoaded(String)} and wrapped in try/catch so
 * the single jar is safe with any subset of the target mods installed, and one broken integration
 * can't take the rest down.
 */
@EmiEntrypoint
public class BerlordsEmiPlugin implements EmiPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("berlords_emi");

    @Override
    public void register(EmiRegistry registry) {
        run("slag", () -> SlagEmiModule.register(registry));
        run("create", () -> CreateEmiModule.register(registry));
        run("cataclysm", () -> CataclysmEmiModule.register(registry));
        run("irons_spellbooks", () -> IronsSpellbooksEmiModule.register(registry));
        run("forbidden_arcanus", () -> ForbiddenArcanusEmiModule.register(registry));
        run("terra_curio", () -> TerraCurioEmiModule.register(registry));
        run("enderio", () -> EnderIOEmiModule.register(registry));
        run("malum", () -> MalumEmiModule.register(registry));
        run("extradelight", () -> ExtraDelightEmiModule.register(registry));
        run("dungeonsdelight", () -> DungeonsDelightEmiModule.register(registry));
        run("expandeddelight", () -> ExpandedDelightEmiModule.register(registry));
        run("avaritia_delight", () -> AvaritiaDelightEmiModule.register(registry));
        run("farmerspizzeria", () -> FarmersPizzeriaEmiModule.register(registry));
        run("youkaisfeasts", () -> YoukaisFeastsEmiModule.register(registry));
        run("cognition", () -> CognitionEmiModule.register(registry));
        run("stellaris", () -> StellarisEmiModule.register(registry));
        run("twilightdelight", () -> TwilightDelightEmiModule.register(registry));
        run("slavic_delight", () -> SlavicDelightEmiModule.register(registry));
        run("cuisinedelight", () -> CuisineDelightEmiModule.register(registry));
        run("berries_and_cherries", () -> BerriesAndCherriesEmiModule.register(registry));
        run("betterarcheology", () -> BetterArcheologyEmiModule.register(registry));
        run("l2complements", () -> L2ComplementsEmiModule.register(registry));
        run("anvilcraft", () -> AnvilCraftEmiModule.register(registry));
    }

    private static void run(String modid, Runnable module) {
        if (!ModList.get().isLoaded(modid)) {
            return;
        }
        try {
            module.run();
        } catch (Throwable t) {
            LOGGER.error("berlords_emi: integration for '{}' failed to register", modid, t);
        }
    }
}
