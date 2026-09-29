package io.github.bertie_mc.fletching.mixin;

import com.fletchery.mod.arrow.ArrowProperties;
import com.fletchery.mod.entity.CustomArrowEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CustomArrowEntity.class, remap = false)
public abstract class WheatFlightMixin extends AbstractArrow {
    @Shadow
    private ArrowProperties props;

    protected WheatFlightMixin(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
    }

    @Inject(method = "resolveProps", at = @At("TAIL"))
    private void bertie$useVanillaGravity(CallbackInfo ci) {
        var tag = ((CustomArrowEntity) (Object) this).getCustomProperties();
        if (io.github.bertie_mc.fletching.ArrowProfile.redesigned(tag))
            setNoGravity(io.github.bertie_mc.fletching.ArrowProfile.read(tag).gravity() == 0);
        else if (props.featherKey.equals("minecraft:wheat")) setNoGravity(props.noGravity);
    }

    @Redirect(
            method = "tick",
            at = @At(value = "FIELD", target = "Lcom/fletchery/mod/arrow/ArrowProperties;gravityMultiplier:F"))
    private float bertie$skipEarlyGravity(ArrowProperties properties) {
        // Vanilla applies gravity after drag. The upstream manual step runs before it.
        return properties.featherKey.equals("minecraft:wheat") ? 1.0F : properties.gravityMultiplier;
    }

    @Override
    protected double getDefaultGravity() {
        double gravity = super.getDefaultGravity();
        var tag = ((CustomArrowEntity) (Object) this).getCustomProperties();
        if (io.github.bertie_mc.fletching.ArrowProfile.redesigned(tag))
            return gravity
                    * io.github.bertie_mc.fletching.ArrowProfile.read(tag).gravity();
        return props != null && props.featherKey.equals("minecraft:wheat")
                ? gravity * props.gravityMultiplier
                : gravity;
    }
}
