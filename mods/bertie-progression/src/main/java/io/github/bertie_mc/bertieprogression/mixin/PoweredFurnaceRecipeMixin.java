package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.recipe.ElectricSmelting;
import java.util.Optional;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "rearth.oritech.block.entity.processing.PoweredFurnaceBlockEntity", remap = false)
public abstract class PoweredFurnaceRecipeMixin {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(
            method = {"serverTick", "getProgress", "getRecipeDuration"},
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/crafting/RecipeManager;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"),
            require = 3,
            remap = false)
    private Optional<?> bertieprogression$electricRecipe(
            RecipeManager recipes, RecipeType type, RecipeInput input, Level level) {
        if (type == RecipeType.SMELTING && input instanceof SingleRecipeInput single) {
            var electric = ElectricSmelting.find(recipes, single, level);
            if (electric.isPresent()) {
                return electric;
            }
        }
        return recipes.getRecipeFor(type, input, level);
    }
}
