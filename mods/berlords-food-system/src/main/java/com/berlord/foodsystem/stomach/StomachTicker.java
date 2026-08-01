package com.berlord.foodsystem.stomach;

import com.berlord.foodsystem.BFS;
import com.berlord.foodsystem.Config;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Per-tick heart of the system: pins the (hidden) hunger bar, ticks slot durations down,
 * applies timeout damage, and handles passive regeneration with its damage cooldown.
 */
@EventBusSubscriber(modid = BFS.MODID)
public final class StomachTicker {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        StomachData data = Stomach.get(player);

        // Hunger is replaced by the stomach; pin it so vanilla never starves or sprint-blocks.
        // An empty stomach (when enabled) instead pins to vanilla's no-sprint threshold (6):
        // vanilla then refuses to start a sprint and cancels an ongoing one, and 6 is still
        // above the 0 that would cause starvation damage. The ticker runs on both sides, so the
        // client's LocalPlayer sees the lowered level and blocks the sprint locally.
        boolean blockSprint = Config.DISABLE_SPRINT_WHEN_EMPTY.get() && Stomach.isEmpty(player);
        player.getFoodData().setFoodLevel(blockSprint ? 6 : 10);

        // ---- slot depletion ----
        // Both sides count down locally; the displayed timer is whole minutes, so instead
        // of syncing every tick the server sends a drift-correcting heartbeat every 30s.
        boolean anyActive = false;
        int unlocked = Stomach.unlockedSlots(player);
        for (int i = 0; i < unlocked; i++) {
            StomachData.FoodSlot slot = data.slots[i];
            if (Double.isInfinite(slot.duration)) continue; // eternal foods never deplete
            if (slot.duration >= 2.0) {
                slot.duration -= 1.0;
                anyActive = true;
            } else if (slot.duration != 0.0) {
                float before = player.getHealth();
                Stomach.clearSlot(player, i);
                float cut = before - player.getHealth();
                if (cut > 0.01F && !player.level().isClientSide()) {
                    Stomach.grantAfterglow(player, cut); // expiry clamp becomes yellow hearts
                }
            }
        }
        if (anyActive && !player.level().isClientSide() && player.tickCount % 600 == 0) {
            data.markDirty();
        }

        // ---- afterglow ledger ----
        if (!player.level().isClientSide()) {
            Stomach.tickAfterglow(player, data);
        }

        // ---- passive regen ----
        if (!player.level().isClientSide() && player.isAlive() && !Config.DISABLE_REGENERATION.get()) {
            if (data.regenCooldown > 0.0) {
                data.regenCooldown -= 1.0; // server-only mechanic, the client never reads it: no sync
            } else if (player.getHealth() < player.getMaxHealth()) {
                data.regenCounter++;
                if (data.regenCounter % (long) Stomach.regenFactor(player) == 0) {
                    player.setHealth(player.getHealth() + 1.0F);
                }
            }
        }

        // ---- sync ----
        if (data.syncDirty && player instanceof ServerPlayer serverPlayer) {
            Stomach.sync(serverPlayer);
            data.syncDirty = false;
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            double cooldown = Config.REGEN_COOLDOWN.get();
            if (cooldown != 0.0) {
                StomachData data = Stomach.get(player);
                data.regenCooldown = Math.clamp(cooldown, 20.0, 600.0);
            }
        }
    }
}
