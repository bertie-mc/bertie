package io.github.bertie_mc.bertieprogression.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * EMI category for one of Bertie Progression's in-world mechanics — Mallet Work, Ominous Fan
 * blowing, Allay corruption. None of them has a GUI, so a category is only an id, an icon and a
 * literal name.
 */
final class InWorldEmiCategory extends EmiRecipeCategory {
    private final Component name;

    InWorldEmiCategory(ResourceLocation id, EmiRenderable icon, Component name) {
        super(id, icon);
        this.name = name;
    }

    @Override
    public Component getName() {
        return name;
    }
}
