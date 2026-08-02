package io.github.bertie_mc.bertieprogression.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** EMI category for an Encased Fan blowing through Twilight Forest's Ominous Fire. */
final class OminousFanEmiCategory extends EmiRecipeCategory {
    private final Component name;

    OminousFanEmiCategory(ResourceLocation id, EmiRenderable icon, Component name) {
        super(id, icon);
        this.name = name;
    }

    @Override
    public Component getName() {
        return name;
    }
}
