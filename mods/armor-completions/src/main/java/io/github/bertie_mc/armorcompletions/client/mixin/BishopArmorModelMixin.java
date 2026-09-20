package io.github.bertie_mc.armorcompletions.client.mixin;

import net.hazen.hazennstuff.Item.Armor.Misc.BishopOfDeceitArmor.BishopOfDeceitArmorModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BishopOfDeceitArmorModel.class, remap = false)
abstract class BishopArmorModelMixin {
    @Inject(method = "getModelResource(Lnet/hazen/hazennstuff/Item/Armor/Misc/BishopOfDeceitArmor/BishopOfDeceitArmorItem;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void armorcompletions$useLegFollowingSkirt(CallbackInfoReturnable<ResourceLocation> callback) {
        callback.setReturnValue(ResourceLocation.fromNamespaceAndPath("hazennstuff", "geo/armor/bishop_of_deceit_completed.geo.json"));
    }
}
