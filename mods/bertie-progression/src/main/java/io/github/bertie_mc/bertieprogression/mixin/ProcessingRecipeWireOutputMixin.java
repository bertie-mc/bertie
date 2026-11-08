package io.github.bertie_mc.bertieprogression.mixin;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import io.github.bertie_mc.bertieprogression.recipe.WireAssemblyResults;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ProcessingRecipe.class, remap = false)
public abstract class ProcessingRecipeWireOutputMixin {
    @Inject(
            method = "rollResults(Ljava/util/List;Lnet/minecraft/util/RandomSource;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false)
    private void bertieprogression$includeGuaranteedWire(
            List<ProcessingOutput> outputs, RandomSource random, CallbackInfoReturnable<List<ItemStack>> cir) {
        cir.setReturnValue(WireAssemblyResults.expand(cir.getReturnValue()));
    }
}
