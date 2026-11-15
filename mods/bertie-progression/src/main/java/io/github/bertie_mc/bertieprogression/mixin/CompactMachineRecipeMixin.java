package io.github.bertie_mc.bertieprogression.mixin;

import com.google.gson.JsonElement;
import io.github.bertie_mc.bertieprogression.recipe.CompactMachineRecipePolicy;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RecipeManager.class)
public abstract class CompactMachineRecipeMixin {
    @ModifyVariable(
            method =
                    "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 1)
    private Map<ResourceLocation, JsonElement> bertieprogression$stockCompactRecipes(
            Map<ResourceLocation, JsonElement> recipes) {
        return CompactMachineRecipePolicy.filter(recipes);
    }
}
