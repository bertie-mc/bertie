package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.ytgld.malstone.items.white.HugeSouls", remap = false)
public abstract class HugeSoulsEnergyMixin {
    @Redirect(
            method = "lLivingDamageEvent",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/resources/ResourceKey;)Z"),
            require = 1)
    private static boolean bertie$energy(DamageSource source, ResourceKey<DamageType> type) {
        return type.equals(DamageTypes.MAGIC) ? DamageFamilies.isEnergy(source) : source.is(type);
    }

    @Redirect(
            method = "lLivingDamageEvent",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/neoforged/neoforge/event/entity/living/LivingDamageEvent$Pre;setNewDamage(F)V"),
            require = 1)
    private static void bertie$contribution(LivingDamageEvent.Pre event, float amount) {
        EnergyLayers.capture(event, amount);
    }
}
