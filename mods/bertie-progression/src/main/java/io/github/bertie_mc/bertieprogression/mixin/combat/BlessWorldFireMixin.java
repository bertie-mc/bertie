package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.CombatTags;
import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "auviotre.enigmatic.legacy.contents.item.books.TheBless$Events", remap = false)
public abstract class BlessWorldFireMixin {
    @Redirect(
            method = "onDamage(Lnet/neoforged/neoforge/event/entity/living/LivingIncomingDamageEvent;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/resources/ResourceKey;)Z"),
            require = 1)
    private static boolean bertie$contactFire(DamageSource source, ResourceKey<DamageType> ignored) {
        return DamageFamilies.isEnergy(source)
                && source.typeHolder().is(CombatTags.CONTACT_FIRE)
                && !source.is(DamageTypes.LAVA);
    }
}
