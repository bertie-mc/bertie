package io.github.bertie_mc.emi;

import io.github.bertie_mc.emi.integration.anvilcraft.AnvilCraftEmiModule;
import io.github.bertie_mc.emi.integration.avaritiadelight.AvaritiaDelightEmiModule;
import io.github.bertie_mc.emi.integration.berriescherries.BerriesAndCherriesEmiModule;
import io.github.bertie_mc.emi.integration.betterarcheology.BetterArcheologyEmiModule;
import io.github.bertie_mc.emi.integration.cataclysm.CataclysmEmiModule;
import io.github.bertie_mc.emi.integration.cognition.CognitionEmiModule;
import io.github.bertie_mc.emi.integration.create.CreateEmiModule;
import io.github.bertie_mc.emi.integration.cuisinedelight.CuisineDelightEmiModule;
import io.github.bertie_mc.emi.integration.dungeonsdelight.DungeonsDelightEmiModule;
import io.github.bertie_mc.emi.integration.enderio.EnderIOEmiModule;
import io.github.bertie_mc.emi.integration.expandeddelight.ExpandedDelightEmiModule;
import io.github.bertie_mc.emi.integration.extradelight.ExtraDelightEmiModule;
import io.github.bertie_mc.emi.integration.farmerspizzeria.FarmersPizzeriaEmiModule;
import io.github.bertie_mc.emi.integration.forbiddenarcanus.ForbiddenArcanusEmiModule;
import io.github.bertie_mc.emi.integration.ironsspellbooks.IronsSpellbooksEmiModule;
import io.github.bertie_mc.emi.integration.l2complements.L2ComplementsEmiModule;
import io.github.bertie_mc.emi.integration.malum.MalumEmiModule;
import io.github.bertie_mc.emi.integration.slag.SlagEmiModule;
import io.github.bertie_mc.emi.integration.slavicdelight.SlavicDelightEmiModule;
import io.github.bertie_mc.emi.integration.stellaris.StellarisEmiModule;
import io.github.bertie_mc.emi.integration.terracurio.TerraCurioEmiModule;
import io.github.bertie_mc.emi.integration.twilightdelight.TwilightDelightEmiModule;
import io.github.bertie_mc.emi.integration.youkaisfeasts.YoukaisFeastsEmiModule;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for all of bertie's native EMI machine integrations. EMI discovers this class by an
 * ASM scan for the {@link EmiEntrypoint} annotation — no service file or mods.toml entrypoint, and
 * the annotation must stay RuntimeInvisible (do NOT add {@code @Retention(RUNTIME)}).
 *
 * <p>Each mod's module is gated behind {@link ModList#isLoaded(String)} and wrapped in try/catch so
 * the single jar is safe with any subset of the target mods installed, and one broken integration
 * can't take the rest down.
 */
@EmiEntrypoint
public class BertieEmiPlugin implements EmiPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("bertieemi");

    @Override
    public void register(EmiRegistry registry) {
        boolean success = true;
        success &= run("slag", () -> SlagEmiModule.register(registry));
        success &= run("create", () -> CreateEmiModule.register(registry));
        success &= run("cataclysm", () -> CataclysmEmiModule.register(registry));
        success &= run("irons_spellbooks", () -> IronsSpellbooksEmiModule.register(registry));
        success &= run("forbidden_arcanus", () -> ForbiddenArcanusEmiModule.register(registry));
        success &= run("terra_curio", () -> TerraCurioEmiModule.register(registry));
        success &= run("enderio", () -> EnderIOEmiModule.register(registry));
        success &= run("malum", () -> MalumEmiModule.register(registry));
        success &= run("extradelight", () -> ExtraDelightEmiModule.register(registry));
        success &= run("dungeonsdelight", () -> DungeonsDelightEmiModule.register(registry));
        success &= run("expandeddelight", () -> ExpandedDelightEmiModule.register(registry));
        success &= run("avaritia_delight", () -> AvaritiaDelightEmiModule.register(registry));
        success &= run("farmerspizzeria", () -> FarmersPizzeriaEmiModule.register(registry));
        success &= run("youkaisfeasts", () -> YoukaisFeastsEmiModule.register(registry));
        success &= run("cognition", () -> CognitionEmiModule.register(registry));
        success &= run("stellaris", () -> StellarisEmiModule.register(registry));
        success &= run("twilightdelight", () -> TwilightDelightEmiModule.register(registry));
        success &= run("slavic_delight", () -> SlavicDelightEmiModule.register(registry));
        success &= run("cuisinedelight", () -> CuisineDelightEmiModule.register(registry));
        success &= run("berries_and_cherries", () -> BerriesAndCherriesEmiModule.register(registry));
        success &= run("betterarcheology", () -> BetterArcheologyEmiModule.register(registry));
        success &= run("l2complements", () -> L2ComplementsEmiModule.register(registry));
        success &= run("anvilcraft", () -> AnvilCraftEmiModule.register(registry));
        if (success) {
            LOGGER.info("Bertie EMI integrations registered successfully");
        }
    }

    private static boolean run(String modid, Runnable module) {
        if (!ModList.get().isLoaded(modid)) {
            return true;
        }
        try {
            module.run();
            return true;
        } catch (Throwable t) {
            LOGGER.error("bertieemi: integration for '{}' failed to register", modid, t);
            return false;
        }
    }
}
