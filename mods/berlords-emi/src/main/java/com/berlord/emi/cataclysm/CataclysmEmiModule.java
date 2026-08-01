package com.berlord.emi.cataclysm;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import com.github.L_Ender.cataclysm.crafting.AltarOfAmethystRecipe;
import com.github.L_Ender.cataclysm.crafting.WeaponfusionRecipe;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Cataclysm: weapon_fusion (Mechanical Fusion Anvil, 2 items -> weapon) and amethyst_bless
 * (Altar of Amethyst, 1 item + time -> item). Results read via the vanilla
 * {@code getResultItem} (fixed stacks, so {@link RegistryAccess#EMPTY} suffices as the provider).
 */
public final class CataclysmEmiModule {
    private CataclysmEmiModule() {
    }

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory fusion = Categories.machine(reg,
                "cataclysm_weapon_fusion", "cataclysm:mechanical_fusion_anvil", "Weapon Fusion");
        Recipes.forEach(reg.getRecipeManager(), WeaponfusionRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.getbaseIngredient()));
            d.itemIn(EmiIngredient.of(r.getAdditionIngredient()));
            d.itemOut(EmiStack.of(r.getResultItem(RegistryAccess.EMPTY)));
            reg.addRecipe(new GenericEmiRecipe(fusion, id, d));
        });

        EmiRecipeCategory amethyst = Categories.machine(reg,
                "cataclysm_amethyst_bless", "cataclysm:altar_of_amethyst", "Amethyst Bless");
        Recipes.forEach(reg.getRecipeManager(), AltarOfAmethystRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) {
                d.itemIn(EmiIngredient.of(ing));
            }
            d.itemOut(EmiStack.of(r.getResultItem(RegistryAccess.EMPTY)));
            if (r.getTime() > 0) {
                d.info(Component.literal(Categories.seconds(r.getTime())));
            }
            reg.addRecipe(new GenericEmiRecipe(amethyst, id, d));
        });
    }
}
