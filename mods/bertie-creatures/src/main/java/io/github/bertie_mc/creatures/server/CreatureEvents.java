package io.github.bertie_mc.creatures.server;

import io.github.bertie_mc.creatures.BertieCreatures;
import io.github.bertie_mc.creatures.server.entity.living.DinosaurEntity;
import io.github.bertie_mc.creatures.server.potion.ACEffectRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = BertieCreatures.MODID)
public final class CreatureEvents {
    private static final String STUNNED = "bertiecreatures:was_stunned";
    private static final ResourceLocation RAGE =
            ResourceLocation.fromNamespaceAndPath(BertieCreatures.MODID, "rage_attack_boost");

    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.hasEffect(ACEffectRegistry.BUBBLED) && living.isInFluidType()) {
            living.removeEffect(ACEffectRegistry.BUBBLED);
        }
        if (living.level().isClientSide) return;
        if (living instanceof Mob mob) {
            boolean stunned = mob.hasEffect(ACEffectRegistry.STUNNED);
            if (!stunned && mob.getPersistentData().getBoolean(STUNNED)) {
                mob.goalSelector.setControlFlag(Goal.Flag.MOVE, true);
                mob.goalSelector.setControlFlag(Goal.Flag.JUMP, true);
                mob.goalSelector.setControlFlag(Goal.Flag.LOOK, true);
                mob.getPersistentData().remove(STUNNED);
            } else if (stunned) {
                mob.getPersistentData().putBoolean(STUNNED, true);
            }
        }
        var attack = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null && !living.hasEffect(ACEffectRegistry.RAGE)) attack.removeModifier(RAGE);
    }

    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof LivingEntity attacker
                && attacker.hasEffect(ACEffectRegistry.STUNNED)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void attack(AttackEntityEvent event) {
        if (event.getTarget() instanceof DinosaurEntity
                && event.getEntity().isPassengerOfSameVehicle(event.getTarget())) {
            event.setCanceled(true);
        }
    }
}
