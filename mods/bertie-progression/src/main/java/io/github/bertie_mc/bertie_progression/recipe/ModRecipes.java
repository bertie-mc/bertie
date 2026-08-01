package io.github.bertie_mc.bertie_progression.recipe;

import io.github.bertie_mc.bertie_progression.BertieProgression;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BertieProgression.MODID);

    /** Shaped crafting with one input returned to the grid instead of consumed. */
    public static final Supplier<CatalystShapedRecipe.Serializer> CATALYST_SHAPED =
            SERIALIZERS.register("catalyst_shaped", CatalystShapedRecipe.Serializer::new);

    private ModRecipes() {}
}
