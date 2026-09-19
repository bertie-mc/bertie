package io.github.bertie_mc.betterhorses.mixin;

import io.github.bertie_mc.betterhorses.HorseEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityLavaMixin {
    @Inject(method = "lavaHurt", at = @At("HEAD"), cancellable = true)
    private void betterhorses$surfaceProtection(CallbackInfo ci) {
        if ((Object) this instanceof Horse horse && HorseEvents.onLavaSurface(horse)) ci.cancel();
    }
}
