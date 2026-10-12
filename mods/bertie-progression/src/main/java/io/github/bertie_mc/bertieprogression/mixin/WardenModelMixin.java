package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.warden.WardenState;
import java.util.List;
import net.minecraft.client.model.WardenModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WardenModel.class)
public abstract class WardenModelMixin {
    @Shadow
    public abstract List<ModelPart> getTendrilsLayerModelParts();

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/monster/warden/Warden;FFFFF)V", at = @At("TAIL"))
    private void bertie$hideTendrils(
            Warden warden, float limbSwing, float limbAmount, float age, float yaw, float pitch, CallbackInfo ci) {
        boolean visible = !((WardenState) warden).bertie$tendrilsSheared();
        getTendrilsLayerModelParts().forEach(part -> part.visible = visible);
    }
}
