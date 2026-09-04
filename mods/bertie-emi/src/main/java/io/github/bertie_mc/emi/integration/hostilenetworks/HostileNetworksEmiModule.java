package io.github.bertie_mc.emi.integration.hostilenetworks;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Hostile Neural Networks. Its two machines are driven by data models held in a datapack registry
 * rather than by recipes, so nothing in the RecipeManager describes them and an EMI pack sees the
 * whole progression line — model, simulation, prediction, fabricated loot — as items with no source.
 *
 * <p>Simulation Chamber: a data model plus that model's own input item yields its base drop and a
 * prediction. Loot Fabricator: a prediction yields one of the model's loot entries, one entry per
 * recipe so each fabricated item is findable on its own.
 */
public final class HostileNetworksEmiModule {

    private static final String NS = "bertieemi";

    private HostileNetworksEmiModule() {}

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory chamber = Categories.machine(
                reg, "hostilenetworks_sim_chamber", "hostilenetworks:sim_chamber", "Simulation Chamber");
        EmiRecipeCategory fabricator = Categories.machine(
                reg, "hostilenetworks_loot_fabricator", "hostilenetworks:loot_fabricator", "Loot Fabricator");

        for (DataModel model : DataModelRegistry.INSTANCE.getValues()) {
            try {
                simulate(reg, chamber, model);
                fabricate(reg, fabricator, model);
            } catch (Throwable ignored) {
                // one malformed model must not cost the rest of the registry
            }
        }
    }

    private static void simulate(EmiRegistry reg, EmiRecipeCategory chamber, DataModel model) {
        MachineDescriptor d = new MachineDescriptor();
        d.itemIn(EmiStack.of(dataModelStack(model)));
        d.itemIn(EmiIngredient.of(model.input()));
        d.itemOut(EmiStack.of(model.baseDrop().copy()));
        d.itemOut(EmiStack.of(model.getPredictionDrop()));
        if (model.simCost() > 0) {
            d.info(Component.literal(model.simCost() + " RF per simulation"));
        }
        reg.addRecipe(new GenericEmiRecipe(chamber, id("sim_chamber", model, -1), d));
    }

    /**
     * One recipe per loot entry. The fabricator picks the entry, so listing them together in a single
     * output row would read as producing all of them at once.
     */
    private static void fabricate(EmiRegistry reg, EmiRecipeCategory fabricator, DataModel model) {
        ItemStack prediction = model.getPredictionDrop();
        for (int index = 0; index < model.fabDrops().size(); index++) {
            ItemStack drop = model.fabDrops().get(index).copy();
            if (drop.isEmpty()) {
                continue;
            }
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiStack.of(prediction.copy()));
            d.itemOut(EmiStack.of(drop));
            reg.addRecipe(new GenericEmiRecipe(fabricator, id("loot_fabricator", model, index), d));
        }
    }

    /** A blank model stamped with this entry, which is how the machine identifies what it is running. */
    private static ItemStack dataModelStack(DataModel model) {
        ItemStack stack = new ItemStack(Hostile.Items.DATA_MODEL);
        DataModelItem.setStoredModel(stack, model);
        DataModelItem.setData(stack, 0);
        return stack;
    }

    /** Models come from a registry, so the ids are synthetic and have to be built from the name. */
    private static ResourceLocation id(String category, DataModel model, int index) {
        String slug =
                model.name().getString().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]+", "_");
        String path = "hostilenetworks/" + category + "/" + slug + (index >= 0 ? "/" + index : "");
        return ResourceLocation.fromNamespaceAndPath(NS, path);
    }
}
