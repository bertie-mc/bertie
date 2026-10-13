package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.recipe.CuttingRecipeAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReloadableServerResources.class)
public abstract class CuttingTagsMixin {
    @Shadow
    public abstract RecipeManager getRecipeManager();

    @Inject(method = "updateRegistryTags()V", at = @At("TAIL"), require = 1)
    private void bertie$boundIngredients(CallbackInfo ci) {
        ((CuttingRecipeAccess) getRecipeManager()).bertie$separateWood();
    }
}
