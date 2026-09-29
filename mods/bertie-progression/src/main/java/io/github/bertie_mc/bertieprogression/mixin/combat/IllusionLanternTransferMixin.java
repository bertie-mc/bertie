package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.DamageFamily;
import io.github.bertie_mc.bertieprogression.combat.DamageRoutes;
import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "auviotre.enigmatic.legacy.contents.item.spellstones.IllusionLantern$Events", remap = false)
public abstract class IllusionLanternTransferMixin {
    @WrapMethod(method = "onDamage")
    private static void bertie$lateShare(LivingDamageEvent.Pre event, Operation<Void> original) {
        if (DamageFamilies.isPure(event.getSource()) || DamageRoutes.redirected(event.getSource())) return;
        var state = EnergyLayers.current(event.getEntity());
        if (state != null && !state.finished()) state.deferTransfer(() -> original.call(event));
        else original.call(event);
    }

    @Redirect(
            method = "onDamage",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            require = 1)
    private static boolean bertie$preserveFamily(
            LivingEntity recipient, DamageSource ignored, float amount, LivingDamageEvent.Pre event) {
        return recipient.hurt(DamageRoutes.transfer(event.getSource()), amount);
    }

    @Inject(method = "onDamageIncoming", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$pure(LivingIncomingDamageEvent event, CallbackInfo ci) {
        if (DamageFamilies.isPure(event.getSource())) ci.cancel();
    }

    @Redirect(
            method = "onDamageIncoming",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"),
            require = 1)
    private static boolean bertie$physicalVulnerability(DamageSource source, TagKey<DamageType> tag) {
        if (tag.location().toString().equals("neoforge:is_magic"))
            return DamageFamilies.of(source) != DamageFamily.PHYSICAL;
        return source.is(tag);
    }
}
