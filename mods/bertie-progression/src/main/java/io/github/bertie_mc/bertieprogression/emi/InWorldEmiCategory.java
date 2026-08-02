package io.github.bertie_mc.bertieprogression.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** EMI category for the Opening Mallet's in-world crafting interactions. */
final class MalletWorkEmiCategory extends EmiRecipeCategory {
    private final Component name;

    MalletWorkEmiCategory(ResourceLocation id, EmiRenderable icon, Component name) {
        super(id, icon);
        this.name = name;
    }

    @Override
    public Component getName() {
        return name;
    }
}
