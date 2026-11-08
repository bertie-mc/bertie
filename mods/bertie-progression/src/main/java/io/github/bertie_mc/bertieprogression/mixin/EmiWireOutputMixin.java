package io.github.bertie_mc.bertieprogression.mixin;

import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.bertieprogression.emi.WireAssemblyEmi;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.emi.emi.api.recipe.BasicEmiRecipe", remap = false)
public abstract class EmiWireOutputMixin {
    @Shadow
    public abstract ResourceLocation getId();

    @Inject(method = "getOutputs", at = @At("RETURN"), cancellable = true, require = 1, remap = false)
    private void bertieprogression$guaranteedWire(CallbackInfoReturnable<List<EmiStack>> cir) {
        cir.setReturnValue(WireAssemblyEmi.outputs(getId(), cir.getReturnValue()));
    }
}
