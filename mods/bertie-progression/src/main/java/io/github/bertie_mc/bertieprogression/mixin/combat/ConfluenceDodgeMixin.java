package io.github.bertie_mc.bertieprogression.mixin.combat;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.confluence.lib.common.LibAttributes", remap = false)
public abstract class ConfluenceDodgeMixin {
    @Shadow
    @Final
    private static Map<Holder<Attribute>, Holder<Attribute>> MAP;

    @Inject(method = "applyDodge", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$oneDodgeRoll(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "prepareReplacements", at = @At("TAIL"), require = 1)
    private static void bertie$independentDodgeContribution(CallbackInfo ci) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("confluence_magic_lib", "generic.dodge_chance");
        for (Holder<Attribute> attribute : MAP.keySet()) {
            if (attribute.is(id)) MAP.put(attribute, attribute);
        }
    }
}
