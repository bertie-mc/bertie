package io.github.bertie_mc.voidfog.mixin;

import io.github.bertie_mc.voidfog.client.VoidFogRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fades the sky towards black along with the fog.
 *
 * <p>Fogging the sky was not enough on its own. Vanilla draws the sky DOME on top of the cleared
 * background, in the biome's own sky colour, and that colour never passes through the fog - so deep
 * underground the terrain went black while any hole through to daylight stayed pale blue and cut a
 * hard bright shape out of the effect.
 *
 * <p>Vanilla's own answer is to skip the dome entirely when the view is foggy, but that is a switch:
 * the sky would vanish between one block of descent and the next. Scaling the colour instead means
 * the opening dims at exactly the rate everything else does, which is the point - there should be no
 * edge anywhere, including this one.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void voidfog$dimSkyWithTheFog(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> callback) {
        if ((Object) this != Minecraft.getInstance().level) {
            return;
        }
        float keep = VoidFogRenderer.skyKept();
        if (keep >= 1.0F) {
            return;
        }
        Vec3 sky = callback.getReturnValue();
        callback.setReturnValue(new Vec3(sky.x * keep, sky.y * keep, sky.z * keep));
    }
}
