package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.advancements.critereon.TagPredicate;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DamageSourcePredicate.class)
public abstract class DamagePredicateFamiliesMixin {
    @Redirect(
            method =
                    "matches(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/damagesource/DamageSource;)Z",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/advancements/critereon/TagPredicate;matches(Lnet/minecraft/core/Holder;)Z"),
            require = 1)
    private boolean bertie$sourceProperties(
            TagPredicate<DamageType> predicate,
            Holder<DamageType> holder,
            ServerLevel level,
            Vec3 position,
            DamageSource source) {
        Boolean routed = DamageFamilies.matches(source, predicate.tag());
        return routed == null ? predicate.matches(holder) : routed == predicate.expected();
    }
}
