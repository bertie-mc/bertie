package io.github.bertie_mc.bertieprogression.combat;

import io.github.bertie_mc.bertieprogression.mixin.combat.DamageStackAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class EnergyLayers {
    private record Resistance(double remaining, double weakening) {}

    private EnergyLayers() {}

    public static CombatHitState state(DamageContainer container) {
        return ((CombatHit) container).bertie$combatState();
    }

    public static CombatHitState current(LivingEntity entity) {
        var stack = ((DamageStackAccessor) entity).bertie$damageContainers();
        return stack == null || stack.isEmpty() ? null : state(stack.peek());
    }

    public static float capture(LivingEntity entity, DamageSource source, float before, float after) {
        CombatHitState state = current(entity);
        if (state == null || state.finished() || !DamageFamilies.isEnergy(source) || before <= 0) {
            return after;
        }
        state.multiplyEnergy(after / before);
        return before;
    }

    public static void capture(LivingDamageEvent.Pre event, float after) {
        event.setNewDamage(capture(event.getEntity(), event.getSource(), event.getNewDamage(), after));
    }

    public static void capture(LivingIncomingDamageEvent event, float after) {
        event.setAmount(capture(event.getEntity(), event.getSource(), event.getAmount(), after));
    }

    public static float finish(LivingEntity target, DamageContainer container, float remaining) {
        CombatHitState state = state(container);
        if (state.finished()) {
            return remaining;
        }
        state.finish();
        DamageSource source = container.getSource();
        DamageFamily family = DamageFamilies.of(source);
        double damage = remaining;
        if (family == DamageFamily.ENERGY) {
            Resistance lodestone = lodestoneResistance(target);
            double multiplier = state.energyRemaining() * lodestone.remaining();
            multiplier = CombatMath.combineRemaining(
                    multiplier, Math.min(0.9, CombatRegistry.value(target, "malstone:magic_res", 0)));
            if (target instanceof InnateEnergyResistance innate) {
                multiplier = CombatMath.combineRemaining(multiplier, innate.bertie$energyResistance());
            }
            damage *= CombatMath.resistanceRemaining(multiplier, lodestone.weakening());
            if (source.getEntity() instanceof LivingEntity attacker) {
                damage *= CombatRegistry.value(attacker, "lodestone:magic_proficiency", 1);
            }
        } else if (family.usesArmor()) {
            damage *= Math.max(0.1, 1.0 - CombatRegistry.value(target, "malstone:damage_res", 0));
        }
        damage = GeneralReductions.apply(target, family, damage);
        container.setNewDamage((float) damage);
        state.applyDefenses();
        damage = container.getNewDamage();
        if (family == DamageFamily.ENERGY) {
            damage = CombatMath.subtractDamage(
                    damage, CombatRegistry.value(target, "additionalentityattributes:generic.magic_protection", 0));
        }
        if (family != DamageFamily.PURE) {
            damage = CombatMath.subtractDamage(damage, state.flatSubtraction());
        }
        container.setNewDamage((float) damage);
        state.applyTransfers();
        state.applyDebts();
        container.setNewDamage(DamagePools.beforeAbsorption(target, source, container.getNewDamage()));
        state.recordHealth(target.getHealth());
        return container.getNewDamage();
    }

    private static Resistance lodestoneResistance(LivingEntity entity) {
        AttributeInstance attribute = CombatRegistry.attribute(entity, "lodestone:magic_resistance");
        if (attribute == null) {
            return new Resistance(1, 0);
        }
        double remaining = 1;
        double weakening = Math.max(0, 1 - attribute.getBaseValue());
        if (attribute.getBaseValue() > 1) {
            remaining = CombatMath.combineRemaining(remaining, attribute.getBaseValue() - 1);
        }
        double stronghold = 0;
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (modifier.amount() < 0) {
                weakening -= modifier.amount();
            } else if (modifier.id().getNamespace().equals("malum")
                    && modifier.id().getPath().startsWith("malignant_stronghold_armor.")) {
                stronghold += modifier.amount();
            } else {
                remaining = CombatMath.combineRemaining(remaining, modifier.amount());
            }
        }
        return new Resistance(CombatMath.combineRemaining(remaining, stronghold), weakening);
    }
}
