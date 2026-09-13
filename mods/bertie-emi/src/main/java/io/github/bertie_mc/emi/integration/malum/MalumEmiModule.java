package io.github.bertie_mc.emi.integration.malum;

import com.sammy.malum.common.data.component.GeasDataComponent;
import com.sammy.malum.common.recipe.RuneworkingRecipe;
import com.sammy.malum.common.recipe.SoulBindingRecipe;
import com.sammy.malum.common.recipe.SpiritFocusingRecipe;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import com.sammy.malum.common.recipe.UnchainedTransmutationRecipe;
import com.sammy.malum.common.recipe.VoidFavorRecipe;
import com.sammy.malum.core.systems.recipe.SpiritIngredient;
import com.sammy.malum.registry.common.item.MalumDataComponents;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import io.github.bertie_mc.emi.framework.Recipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Malum's magic crafting. Spirit ingredients render as their shard item with the required count.
 * Node smelting/blasting already show in EMI's vanilla furnace categories, and spirit_repair
 * (damaged->repaired N-rows) stays deferred. Reads each recipe's own public fields per
 * research/malum-recipe-spec.md.
 *
 * <p>Soul binding produces a GeasEffectType rather than an item, which is why it had no entry: a geas
 * is worn in a curio slot, not crafted into the inventory. The output is shown as the Geas item
 * carrying that effect in its data component, which is the same stack the brazier hands back, so it
 * carries the geas's real name and tooltip instead of a placeholder.
 */
public final class MalumEmiModule {
    private MalumEmiModule() {}

    public static void register(EmiRegistry reg) {
        RecipeManager rm = reg.getRecipeManager();

        EmiRecipeCategory infusion =
                Categories.machine(reg, "malum_spirit_infusion", "malum:spirit_altar", "Spirit Infusion");
        Recipes.forEach(rm, SpiritInfusionRecipe.class, (id, r) -> {
            EmiIngredient primary = NeoForgeEmiIngredient.of(r.input);
            List<EmiIngredient> extras = new ArrayList<>();
            for (SizedIngredient si : r.extraInputs) {
                extras.add(NeoForgeEmiIngredient.of(si));
            }
            List<EmiIngredient> spirits = new ArrayList<>();
            for (SpiritIngredient sp : r.spirits) {
                spirits.add(spirit(sp));
            }
            // Bespoke altar layout (columns) instead of the generic single row — see SpiritInfusionEmiRecipe.
            reg.addRecipe(new SpiritInfusionEmiRecipe(infusion, id, primary, extras, spirits, EmiStack.of(r.result)));
        });

        EmiRecipeCategory rune = Categories.machine(reg, "malum_runeworking", "malum:runic_workbench", "Runeworking");
        Recipes.forEach(rm, RuneworkingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(NeoForgeEmiIngredient.of(r.input));
            d.itemIn(NeoForgeEmiIngredient.of(r.secondaryInput));
            d.itemOut(EmiStack.of(r.output));
            reg.addRecipe(new GenericEmiRecipe(rune, id, d));
        });

        EmiRecipeCategory focus =
                Categories.machine(reg, "malum_spirit_focusing", "malum:spirit_crucible", "Spirit Focusing");
        Recipes.forEach(rm, SpiritFocusingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.input));
            for (SpiritIngredient sp : r.spirits) {
                d.itemIn(spirit(sp));
            }
            d.itemOut(EmiStack.of(r.output));
            if (r.time > 0) {
                d.info(Component.literal(Categories.seconds(r.time)));
            }
            reg.addRecipe(new GenericEmiRecipe(focus, id, d));
        });

        EmiRecipeCategory trans =
                Categories.machine(reg, "malum_transmutation", "malum:arcane_spirit", "Spirit Transmutation");
        Recipes.forEach(rm, UnchainedTransmutationRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.ingredient));
            d.itemOut(EmiStack.of(r.output));
            reg.addRecipe(new GenericEmiRecipe(trans, id, d));
        });

        EmiRecipeCategory binding =
                Categories.machine(reg, "malum_soul_binding", "malum:soulbinding_brazier", "Soul Binding");
        Recipes.forEach(rm, SoulBindingRecipe.class, (id, r) -> {
            ItemStack geas = geas(r);
            if (geas.isEmpty()) {
                return;
            }
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(NeoForgeEmiIngredient.of(r.input));
            for (SizedIngredient si : r.extraInputs) {
                d.itemIn(NeoForgeEmiIngredient.of(si));
            }
            for (SpiritIngredient sp : r.spirits) {
                d.itemIn(spirit(sp));
            }
            d.itemOut(EmiStack.of(geas));
            d.info(Component.literal("Light the brazier with the items around it, then swear the geas"));
            d.info(Component.literal("it hands back to bind it to a geas slot"));
            reg.addRecipe(new GenericEmiRecipe(binding, id, d));
        });

        EmiRecipeCategory well = Categories.machine(reg, "malum_void_favor", "malum:void_depot", "Weeping Well");
        Recipes.forEach(rm, VoidFavorRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.input));
            d.itemOut(EmiStack.of(r.result));
            reg.addRecipe(new GenericEmiRecipe(well, id, d));
        });
    }

    /** The unsworn geas the brazier yields: the mod's own item, stamped with which geas it carries. */
    private static ItemStack geas(SoulBindingRecipe recipe) {
        ItemStack stack =
                new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("malum", "geas")));
        if (stack.isEmpty() || recipe.result == null) {
            return ItemStack.EMPTY;
        }
        stack.set(MalumDataComponents.GEAS_EFFECT.get(), new GeasDataComponent(recipe.result, false));
        return stack;
    }

    private static EmiIngredient spirit(SpiritIngredient sp) {
        return EmiStack.of(sp.asItemStack()).setAmount(Math.max(1, sp.count()));
    }
}
