package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.enchanting.RavenInfusion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe", remap = false)
public abstract class RavenInfusionRecipeMixin {
    @Shadow
    @Final
    protected ItemStack output;

    @Shadow
    @Final
    protected Ingredient input;

    @Inject(
            method = "matches(Lnet/minecraft/world/item/ItemStack;FFF)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void bertie$ravenStats(
            ItemStack stack, float eterna, float quanta, float arcana, CallbackInfoReturnable<Boolean> cir) {
        if (RavenInfusion.isOutput(output)) {
            cir.setReturnValue(input.test(stack) && RavenInfusion.matchesStats(eterna, quanta, arcana));
        }
    }
}
