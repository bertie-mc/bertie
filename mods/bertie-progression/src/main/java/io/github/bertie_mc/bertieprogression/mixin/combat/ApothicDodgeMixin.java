package io.github.bertie_mc.bertieprogression.mixin.combat;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apothic_attributes.impl.AttributeEvents", remap = false)
public abstract class ApothicDodgeMixin {
    @Inject(method = "isDodging", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$oneDodgeRoll(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
