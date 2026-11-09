package io.github.bertie_mc.bertieprogression.mixin;

import com.mojang.datafixers.util.Either;
import io.github.bertie_mc.bertieprogression.compat.ManaPotionFluids;
import java.util.List;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "rearth.oritech.util.FluidIngredient", remap = false)
public abstract class OritechManaPotionMixin {
    @Shadow
    public abstract Either<TagKey<Fluid>, ResourceLocation> fluidContent();

    @Inject(method = "matchesFluid(Ldev/architectury/fluid/FluidStack;)Z",
            at = @At("RETURN"), cancellable = true, require = 1, remap = false)
    private void bertieprogression$matchPotion(@Coerce Object stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && bertieprogression$isMana()) {
            cir.setReturnValue(stack instanceof DataComponentHolder components && ManaPotionFluids.matches(components));
        }
    }

    @Inject(method = "getFluidStacks", at = @At("RETURN"), require = 1, remap = false)
    private void bertieprogression$showManaPotion(CallbackInfoReturnable<List<?>> cir) {
        if (bertieprogression$isMana()) {
            cir.getReturnValue().forEach(ManaPotionFluids::decoratePreview);
        }
    }

    private boolean bertieprogression$isMana() {
        return fluidContent().left().map(tag -> tag.location().equals(ManaPotionFluids.TAG)).orElse(false);
    }
}
