package io.github.bertie_mc.bertieprogression.combat;

import io.github.bertie_mc.bertieprogression.mixin.combat.DamageStackAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

public final class DamagePools {
    private DamagePools() {}

    public static float beforeAbsorption(LivingEntity target, DamageSource source, float damage) {
        if (damage > 0 && ModList.get().isLoaded("enigmaticlegacyplus")) {
            return LanternPool.consume(target, source, damage);
        }
        return damage;
    }

    public static float afterAbsorption(LivingEntity target, DamageSource source, float damage) {
        CombatHitState state = EnergyLayers.current(target);
        if (state == null || state.dikeProcessed()) return damage;
        state.markDikeProcessed();
        if (damage > 0 && ModList.get().isLoaded("pastel")) {
            damage = AzurePool.consume(target, source, damage);
        }
        var stack = ((DamageStackAccessor) target).bertie$damageContainers();
        if (stack != null && !stack.isEmpty()) stack.peek().setNewDamage(damage);
        return damage;
    }
}
