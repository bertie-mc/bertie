package io.github.bertie_mc.bertieprogression.mixin;

import java.util.Map;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Generated guides use names from the active locale rather than fixed datagen text. */
@Pseudo
@Mixin(targets = "net.createmod.ponder.foundation.registration.PonderLocalization", remap = false)
public abstract class SpatialPonderTextMixin {
    @Shadow
    @Final
    public Map<ResourceLocation, Map<String, String>> specific;

    @Inject(
            method = "getSpecific(Lnet/minecraft/resources/ResourceLocation;Ljava/lang/String;)Ljava/lang/String;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void bertieprogression$guideNames(
            ResourceLocation id, String key, CallbackInfoReturnable<String> callback) {
        if (!id.getNamespace().equals("bertieprogression") || !id.getPath().startsWith("guide/")) return;
        if (I18n.exists(id.getNamespace() + ".ponder." + id.getPath() + "." + key)) return;
        String name = specific.getOrDefault(id, Map.of()).get(key);
        if (name != null) callback.setReturnValue(name);
    }
}
