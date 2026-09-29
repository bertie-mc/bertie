package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.CombatRegistry;
import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.player.ServerPlayerEvents", remap = false)
public abstract class HeartstopDebtMixin {
    @Shadow
    public static void onBeforeDamageTaken(LivingDamageEvent.Pre event) {
        throw new AssertionError();
    }

    @Inject(method = "onBeforeDamageTaken", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$deferHeartstop(LivingDamageEvent.Pre event, CallbackInfo ci) {
        var state = EnergyLayers.current(event.getEntity());
        if (state != null
                && !state.applyingDebts()
                && !state.finished()
                && !DamageFamilies.isPure(event.getSource())
                && CombatRegistry.effectLevel(event.getEntity(), "irons_spellbooks:heartstop") > 0) {
            state.deferDebt(() -> onBeforeDamageTaken(event));
            ci.cancel();
        }
    }

    @Redirect(
            method = "onBeforeDamageTaken",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/neoforged/neoforge/event/entity/living/LivingDamageEvent$Pre;getOriginalDamage()F"),
            require = 1)
    private static float bertie$actualDeferredDamage(LivingDamageEvent.Pre event) {
        return event.getNewDamage();
    }

    @Redirect(
            method = "onBeforeDamageTaken",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"),
            require = 1)
    private static boolean bertie$pureCannotBeDeferred(
            LivingEntity entity, Holder<MobEffect> effect, LivingDamageEvent.Pre event) {
        return !(effect.is(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "heartstop"))
                        && DamageFamilies.isPure(event.getSource()))
                && entity.hasEffect(effect);
    }
}
