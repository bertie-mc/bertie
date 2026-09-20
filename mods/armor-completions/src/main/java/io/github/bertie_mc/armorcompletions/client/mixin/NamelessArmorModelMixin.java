package io.github.bertie_mc.armorcompletions.client.mixin;

import net.hazen.hazennstuff.Item.Armor.Misc.NamelessOneArmor.NamelessOneArmorModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NamelessOneArmorModel.class, remap = false)
abstract class NamelessArmorModelMixin {
    @Inject(method = "getModelResource(Lnet/hazen/hazennstuff/Item/Armor/Misc/NamelessOneArmor/NamelessOneArmorItem;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void armorcompletions$useCorrectLegBindings(CallbackInfoReturnable<ResourceLocation> callback) {
        callback.setReturnValue(ResourceLocation.fromNamespaceAndPath("hazennstuff", "geo/armor/nameless_one_completed.geo.json"));
    }
}
