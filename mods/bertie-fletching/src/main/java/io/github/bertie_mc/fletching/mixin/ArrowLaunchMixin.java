package io.github.bertie_mc.fletching.mixin;

import com.fletchery.mod.entity.CustomArrowEntity;
import io.github.bertie_mc.fletching.ArrowProfile;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Projectile.class, remap = false)
public abstract class ArrowLaunchMixin {
    @Inject(method = "shoot", at = @At("TAIL"))
    private void bertie$consistentLaunchSpeed(
            double x, double y, double z, float speed, float spread, CallbackInfo ci) {
        if ((Object) this instanceof CustomArrowEntity arrow && ArrowProfile.redesigned(arrow.getCustomProperties()))
            arrow.setDeltaMovement(arrow.getDeltaMovement().normalize().scale(speed));
    }
}
