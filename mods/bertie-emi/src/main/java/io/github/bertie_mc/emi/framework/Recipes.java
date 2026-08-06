package io.github.bertie_mc.emi.framework;

import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

/** Enumerate loaded recipes by concrete class — avoids needing each mod's RecipeType/registry holder. */
public final class Recipes {
    private Recipes() {}

    public static <T> void forEach(RecipeManager rm, Class<T> cls, BiConsumer<ResourceLocation, T> fn) {
        for (RecipeHolder<?> h : rm.getRecipes()) {
            Object v = h.value();
            if (cls.isInstance(v)) {
                try {
                    fn.accept(h.id(), cls.cast(v));
                } catch (Throwable ignored) {
                    // one malformed recipe must not sink the category
                }
            }
        }
    }
}
