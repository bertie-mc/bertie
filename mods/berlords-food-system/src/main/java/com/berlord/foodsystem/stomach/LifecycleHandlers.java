package com.berlord.foodsystem.stomach;

import com.berlord.foodsystem.BFS;
import com.berlord.foodsystem.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Login/base health, death reset, dimension-change sync, clone copy, sleep depletion,
 * cake-slice healing, natural-regen gamerule.
 */
@EventBusSubscriber(modid = BFS.MODID)
public final class LifecycleHandlers {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        applyBaseHealth(player);
        Stomach.updateHealth(player);
        Stomach.clampHealth(player);
        Stomach.sync(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Stomach.reset(player, false); // death empties the stomach (eternal foods & unlocked slots are kept)
        applyBaseHealth(player);
        Stomach.updateHealth(player);
        player.setHealth(player.getMaxHealth());
        Stomach.sync(player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Stomach.sync(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        StomachData original = event.getOriginal().getData(BFS.STOMACH);
        event.getEntity().getData(BFS.STOMACH).copyFrom(original);
    }

    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        if (!Config.SLEEP_DEPLETION.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return; // server-authoritative; client gets the sync
        StomachData data = Stomach.get(player);
        boolean changed = false;
        for (StomachData.FoodSlot slot : data.slots) {
            if (Double.isInfinite(slot.duration)) continue; // eternal foods are never depleted
            if (slot.duration >= 2.0) {
                slot.duration = Math.ceil(slot.duration * 0.5);
                changed = true;
            }
        }
        if (changed) data.markDirty();
    }

    /** eating a slice from a cake-like block heals 2 hearts (the slice never hits a slot) */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var level = event.getLevel();
        BlockPos pos = event.getPos();
        var before = level.getBlockState(pos);
        if (!before.is(Stomach.SLICEABLE_BLOCK)) return;
        var server = player.getServer();
        if (server == null) return;
        // The block's own use (eating a slice) runs AFTER this event resolves, so defer the
        // heal to end of tick and grant it only if the block actually changed — a bite advanced
        // its state or the last slice removed it. This skips no-op clicks such as sneaking with
        // a placeable item, where the cake is untouched and no slice is consumed.
        server.execute(() -> {
            if (player.isAlive() && level.getBlockState(pos) != before) {
                player.setHealth(player.getHealth() + 4.0F);
            }
        });
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (Config.DISABLE_NATURAL_REGEN_RULE.get() && event.getLevel().getServer() != null) {
            event.getLevel().getLevelData().getGameRules()
                    .getRule(GameRules.RULE_NATURAL_REGENERATION)
                    .set(false, event.getLevel().getServer());
        }
    }

    private static void applyBaseHealth(Player player) {
        var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(Math.clamp(Config.BASE_HEALTH.get(), 2.0, 20.0));
        }
    }
}
