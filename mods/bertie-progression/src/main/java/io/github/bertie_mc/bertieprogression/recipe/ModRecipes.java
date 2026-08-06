package io.github.bertie_mc.bertieprogression.recipe;

import io.github.bertie_mc.bertieprogression.BertieProgression;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BertieProgression.MODID);

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, BertieProgression.MODID);

    /** Shaped crafting with one input returned to the grid instead of consumed. */
    public static final Supplier<CatalystShapedRecipe.Serializer> CATALYST_SHAPED =
            SERIALIZERS.register("catalyst_shaped", CatalystShapedRecipe.Serializer::new);

    /** Encased Fan blowing through Twilight Forest's Ominous Fire. */
    public static final Supplier<OminousFanRecipe.Serializer> OMINOUS_FAN =
            SERIALIZERS.register("ominous_fan", OminousFanRecipe.Serializer::new);

    public static final Supplier<RecipeType<OminousFanRecipe>> OMINOUS_FAN_TYPE = TYPES.register(
            "ominous_fan",
            () -> RecipeType.simple(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    BertieProgression.MODID, "ominous_fan")));

    private ModRecipes() {}
}
