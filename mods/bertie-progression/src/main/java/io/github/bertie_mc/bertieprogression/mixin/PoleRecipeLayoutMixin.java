package io.github.bertie_mc.bertieprogression.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(
        targets = "committee.nova.mods.avaritia.init.compat.emi.category.tables.NetherCraftingTableCategory",
        remap = false)
public abstract class PoleRecipeLayoutMixin {
    @Shadow
    @Final
    private RecipeHolder<?> recipe;

    @ModifyExpressionValue(
            method = "addWidgets",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;floorDiv(II)I", ordinal = 0),
            require = 1)
    private int bertieprogression$lowerPole(int original) {
        return recipe.id().equals(ResourceLocation.parse("oritech:crafting/_pole"))
                        && recipe.value().getIngredients().size() == 20
                        && recipe.value().canCraftInDimensions(5, 4)
                ? 1
                : original;
    }
}
