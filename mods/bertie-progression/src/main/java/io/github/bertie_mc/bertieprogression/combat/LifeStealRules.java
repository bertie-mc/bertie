package io.github.bertie_mc.bertieprogression.combat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class LifeStealRules {
    private LifeStealRules() {}

    public static boolean eligible(LivingEntity target, DamageSource source) {
        return !(target instanceof ArmorStand)
                && !BuiltInRegistries.ENTITY_TYPE
                        .getKey(target.getType())
                        .getNamespace()
                        .equals("dummmmmmy")
                && source.getDirectEntity() instanceof LivingEntity
                && source.getDirectEntity() == source.getEntity()
                && source.getEntity() != target
                && source.isDirect()
                && DamageFamilies.of(source) == DamageFamily.PHYSICAL
                && !DamageFamilies.isDot(source)
                && !EffectDamageContext.isDot(target)
                && !source.is(DamageTypeTags.IS_PROJECTILE)
                && !source.is(DamageTypeTags.IS_EXPLOSION);
    }

    public static void collect(LivingDamageEvent.Post event, float healing) {
        if (!eligible(event.getEntity(), event.getSource()) || event.getNewDamage() <= 0) {
            return;
        }
        CombatHitState state = EnergyLayers.current(event.getEntity());
        if (state != null) {
            state.addLifeSteal(healing / event.getNewDamage());
        }
    }

    public static void finish(LivingEntity target, DamageContainer container) {
        DamageSource source = container.getSource();
        if (!eligible(target, source)) {
            return;
        }
        LivingEntity attacker = (LivingEntity) source.getDirectEntity();
        CombatHitState state = EnergyLayers.state(container);
        double apothic = CombatRegistry.value(attacker, "apothic_attributes:life_steal", 0);
        double rate = (apothic > 0.001 ? apothic : 0)
                + CombatRegistry.value(attacker, "enigmaticlegacyplus:lifesteal", 0)
                + state.extraLifeSteal();
        double damage = CombatMath.eligibleLifeStealDamage(container.getNewDamage(), state.healthBefore());
        if (rate > 0 && damage > 0) {
            attacker.heal((float) (rate * damage));
        }
    }
}
