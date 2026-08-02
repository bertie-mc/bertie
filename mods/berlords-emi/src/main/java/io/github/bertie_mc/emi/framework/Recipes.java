package io.github.bertie_mc.emi.framework;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.function.BiConsumer;

/** Enumerate loaded recipes by concrete class — avoids needing each mod's RecipeType/registry holder. */
public final class Recipes {
    private Recipes() {
    }

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

    /**
     * Enumerate recipes by their registered RecipeType id instead of by concrete class.
     *
     * <p>Use this for a recipe type that lives in one of our OWN sibling mods. Matching on the
     * class would force a compile dependency between two separately published mods - which nothing
     * else in this module does - whereas the type id plus vanilla's {@code Recipe} interface is
     * enough to read ingredients and result.
     */
    public static void forEachOfType(RecipeManager rm, ResourceLocation typeId,
                                     BiConsumer<ResourceLocation, Recipe<?>> fn) {
        for (RecipeHolder<?> h : rm.getRecipes()) {
            Recipe<?> r = h.value();
            ResourceLocation id = BuiltInRegistries.RECIPE_TYPE.getKey(r.getType());
            if (typeId.equals(id)) {
                try {
                    fn.accept(h.id(), r);
                } catch (Throwable ignored) {
                    // one malformed recipe must not sink the category
                }
            }
        }
    }
}
