package io.github.bertie_mc.betterhorses.mixin;

import io.github.bertie_mc.betterhorses.HorseEquipment;
import io.github.bertie_mc.betterhorses.SpecialSaddleItem;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorseModel.class)
public abstract class HorseSaddleModelMixin {
    @Shadow
    @Final
    protected ModelPart body;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/animal/horse/AbstractHorse;FFFFF)V", at = @At("TAIL"))
    private void betterhorses$replaceVanillaSaddle(
            AbstractHorse horse, float swing, float amount, float age, float yaw, float pitch, CallbackInfo ci) {
        if (horse instanceof Horse
                && ((HorseEquipment) horse).betterhorses$syncedSaddle().getItem() instanceof SpecialSaddleItem)
            body.getChild("saddle").visible = false;
    }
}
