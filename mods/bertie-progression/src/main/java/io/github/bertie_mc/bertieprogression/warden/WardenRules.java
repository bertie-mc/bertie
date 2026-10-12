package io.github.bertie_mc.bertieprogression.warden;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class WardenRules {
    public static final ResourceLocation TENDRIL = ResourceLocation.parse("apothic_enchanting:warden_tendril");
    public static final int RETALIATION_COOLDOWN = 200;

    private WardenRules() {}

    public static boolean shouldRetaliate(double distanceSquared, long tick, long readyAt) {
        return distanceSquared > 144 && tick >= readyAt;
    }

    @SubscribeEvent
    public static void shear(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Warden warden) || !warden.isAlive()) return;
        ItemStack tool = event.getItemStack();
        WardenState state = (WardenState) warden;
        var tendril = BuiltInRegistries.ITEM.get(TENDRIL);
        if (state.bertie$tendrilsSheared()
                || !tool.canPerformAction(ItemAbilities.SHEARS_HARVEST)
                || tendril == Items.AIR) return;
        event.setCanceled(true);
        event.setCancellationResult(
                InteractionResult.sidedSuccess(warden.level().isClientSide()));
        if (warden.level().isClientSide()) return;
        state.bertie$setTendrilsSheared(true);
        warden.spawnAtLocation(tendril);
        warden.playSound(SoundEvents.SHEEP_SHEAR, 1, 1);
        warden.gameEvent(GameEvent.SHEAR, event.getEntity());
        tool.hurtAndBreak(
                1,
                event.getEntity(),
                event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND);
    }

    @SubscribeEvent
    public static void retaliate(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Warden warden)
                || !warden.isAlive()
                || !(warden.level() instanceof ServerLevel level)
                || event.getNewDamage() <= 0
                || !(event.getSource().getEntity() instanceof Player attacker)
                || !attacker.isAlive()
                || attacker.level() != level) return;
        WardenState state = (WardenState) warden;
        long tick = level.getGameTime();
        if (!shouldRetaliate(warden.distanceToSqr(attacker), tick, state.bertie$retaliationReadyAt())) return;
        state.bertie$setRetaliationReadyAt(tick + RETALIATION_COOLDOWN);
        warden.getLookControl().setLookAt(attacker, 180, 180);
        level.broadcastEntityEvent(warden, (byte) 62);
        Vec3 start = warden.position().add(0, 1.6, 0);
        Vec3 delta = attacker.getEyePosition().subtract(start);
        int segments = Math.min(64, Math.max(1, (int) Math.ceil(delta.length())));
        for (int i = 0; i <= segments; i++) {
            Vec3 point = start.add(delta.scale((double) i / segments));
            level.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, warden.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3, 1);
        if (attacker instanceof ServerPlayer player && warden.distanceToSqr(player) > 2304) {
            player.playNotifySound(SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1, 1);
        }
        DamageSource normal = level.damageSources().sonicBoom(warden);
        attacker.hurt(new RetaliationDamageSource(normal, warden), 20);
        double resistance = 1 - attacker.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        Vec3 direction = delta.normalize();
        attacker.push(direction.x * 2.5 * resistance, direction.y * 0.5 * resistance, direction.z * 2.5 * resistance);
    }
}
