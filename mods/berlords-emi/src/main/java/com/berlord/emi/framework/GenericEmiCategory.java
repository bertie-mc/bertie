package com.berlord.emi.framework;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A machine category whose only customization is its id, workstation icon, and display name.
 * We override {@link #getName()} so the label is explicit rather than relying on EMI's
 * {@code emi.category.<ns>.<path>} translation-key convention.
 */
public class GenericEmiCategory extends EmiRecipeCategory {
    private final Component name;

    public GenericEmiCategory(ResourceLocation id, EmiRenderable icon, Component name) {
        super(id, icon);
        this.name = name;
    }

    @Override
    public Component getName() {
        return name;
    }
}
