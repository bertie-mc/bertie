package io.github.bertie_mc.bertieprogression.mixin;

import java.util.Arrays;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.dubhe.anvilcraft.recipe.anvil.wrap.VanillaRecipesWrap", remap = false)
public abstract class AnvilRecyclingMixin {
    @Inject(
            method =
                    "wrap(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/world/item/crafting/SmeltingRecipe;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private static void bertie$skipSmeltingRecovery(
            HolderLookup.Provider registries, SmeltingRecipe recipe, CallbackInfo ci) {
        if (equipmentInput(recipe)) ci.cancel();
    }

    @Inject(
            method =
                    "wrap(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/world/item/crafting/BlastingRecipe;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private static void bertie$skipBlastingRecovery(
            HolderLookup.Provider registries, BlastingRecipe recipe, CallbackInfo ci) {
        if (equipmentInput(recipe)) ci.cancel();
    }

    private static boolean equipmentInput(AbstractCookingRecipe recipe) {
        return recipe != null
                && recipe.getIngredients().stream()
                        .flatMap(ingredient -> Arrays.stream(ingredient.getItems()))
                        .anyMatch(stack -> stack.isDamageableItem()
                                || stack.getItem() instanceof TieredItem
                                || stack.getItem() instanceof ArmorItem
                                || stack.getItem() instanceof AnimalArmorItem);
    }
}
