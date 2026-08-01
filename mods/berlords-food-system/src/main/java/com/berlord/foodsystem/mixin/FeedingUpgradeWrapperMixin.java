package com.berlord.foodsystem.mixin;

import com.berlord.foodsystem.BFS;
import com.berlord.foodsystem.Config;
import com.berlord.foodsystem.compat.FeedingContext;
import com.berlord.foodsystem.compat.SBCompat;
import com.berlord.foodsystem.stomach.Stomach;
import com.berlord.foodsystem.stomach.StomachData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.IntConsumer;

/**
 * Slot-aware gate for Sophisticated Backpacks feeding upgrades: the stomach pins the
 * hunger bar at 10, so an ungated upgrade would burn a food item every few seconds.
 * Feeding is allowed only into a free slot or as a near-expiry same-food refresh.
 *
 * Also: eternal foods are never auto-fed (manual choice only), the Advanced Feeding
 * Upgrade is dead (the item is disabled while this mod is installed), and the feeder is
 * PARKED when there is nothing to do (config cadence, 580: a refresh always lands inside
 * the 600t window). Instead of polling fast "just in case", we force a scan on the next
 * tick when something actually changes:
 *  - the backpack contents change (a real InventoryHandler listener, gated so we ignore
 *    our own eating and non-food edits), or
 *  - a stomach event frees/adds a slot (Demonic Gruel, Emetic, expiry, Stomach Extension),
 *    surfaced as a bumped StomachData.feedWakeStamp.
 * The slow cadence remains as a backstop for the few opportunities neither wake can see
 * (the silent refresh-window crossing, config/tag reloads, SB upgrade-GUI filter changes).
 *
 * Applied only when sophisticatedcore is installed (see BfsMixinPlugin).
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeWrapper", remap = false)
public abstract class FeedingUpgradeWrapperMixin {

    @Inject(method = "tryFeedingStack", at = @At("HEAD"), cancellable = true)
    private void bfs$gateFeeding(Level level, int hungerLevel, Player player, Integer slot, ItemStack stack,
                                 ITrackedContentsItemHandler inventory, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.SB_FEEDING.get() || SBCompat.isAdvancedFeedingUpgrade(((IUpgradeWrapper) this).getUpgradeStack())) {
            cir.setReturnValue(false);
            return;
        }
        if (!stack.has(DataComponents.FOOD) || stack.is(Stomach.NOT_FOOD)
                || stack.has(BFS.ETERNAL.get())) { // eternal foods are eaten by choice, never by machine
            cir.setReturnValue(false);
            return;
        }

        int sameSlot = Stomach.findSlotWith(player, stack);
        boolean refreshable = sameSlot >= 0
                && !Stomach.isEternal(Stomach.get(player).slots[sameSlot].stack)
                && Stomach.get(player).slots[sameSlot].duration <= Config.SAME_FOOD_REFRESH_TICKS.get();
        boolean freeSlot = sameSlot < 0 && Stomach.findEmptySlot(player) >= 0;

        if (freeSlot || refreshable) {
            FeedingContext.begin(); // cleared in bfs$endFeeding
        } else {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tryFeedingStack", at = @At("RETURN"))
    private void bfs$endFeeding(Level level, int hungerLevel, Player player, Integer slot, ItemStack stack,
                                ITrackedContentsItemHandler inventory, CallbackInfoReturnable<Boolean> cir) {
        FeedingContext.end();
    }

    @Unique
    private static final ThreadLocal<Player> bfs$tickPlayer = new ThreadLocal<>();

    /** stomach stamp last consumed by this wrapper; a mismatch means a slot opened/closed */
    @Unique
    private long bfs$lastWakeStamp = 0;
    /** set by the contents listener / stomach-stamp check; honored (with an opportunity gate) at tick HEAD */
    @Unique
    private boolean bfs$wakePending = false;
    /** the handler our listener is attached to; re-register only when this instance changes */
    @Unique
    private InventoryHandler bfs$listenedHandler = null;

    @Inject(method = "tick", at = @At("HEAD"))
    private void bfs$onTickHead(Entity entity, Level level, BlockPos pos, CallbackInfo ci) {
        Player player = entity instanceof Player p ? p : null;
        bfs$tickPlayer.set(player);
        if (player == null) {
            return; // placed-backpack / non-player path: cadence-only, no per-player wake
        }

        bfs$ensureListener();

        // a freed/added stomach slot (expiry, gruel, emetic, extension) bumps feedWakeStamp
        long stamp = Stomach.get(player).feedWakeStamp;
        if (stamp != bfs$lastWakeStamp) {
            bfs$lastWakeStamp = stamp;
            bfs$wakePending = true;
        }

        // honor a pending wake only when a slot can actually accept food right now,
        // so churning a backpack full of non-eligible items can't spin the feeder
        if (bfs$wakePending) {
            bfs$wakePending = false;
            if (bfs$hasFeedOpportunity(player)) {
                ((UpgradeWrapperBaseAccessor) (Object) this).bfs$setCooldown(0L); // -> isInCooldown false -> scans this tick
            }
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void bfs$clearTickPlayer(Entity entity, Level level, BlockPos pos, CallbackInfo ci) {
        bfs$tickPlayer.remove();
    }

    /**
     * Keep an inventory-change listener attached to the handler the feeder iterates. The
     * processing handler is the plain InventoryHandler unless an Inception upgrade wraps it
     * (then it's not an InventoryHandler and has no addListener) — in that case we skip and
     * lean on the slow cadence. SB wipes/rebuilds the handler on upgrade refresh; we re-attach
     * by identity (a clear-without-rebuild silently drops us until the next rebuild, which the
     * cadence backstop covers).
     */
    @Unique
    private void bfs$ensureListener() {
        IStorageWrapper sw = ((UpgradeWrapperBaseAccessor) (Object) this).bfs$storageWrapper();
        ITrackedContentsItemHandler proc = sw.getInventoryForUpgradeProcessing();
        if (!(proc instanceof InventoryHandler handler) || handler == bfs$listenedHandler) {
            return;
        }
        bfs$listenedHandler = handler;
        final InventoryHandler target = handler;
        IntConsumer listener = changedSlot -> {
            if (FeedingContext.isActive()) {
                return; // our own consumption, not an external change
            }
            ItemStack s = target.getStackInSlot(changedSlot);
            if (Stomach.isFoodForSystem(s) && !s.has(BFS.ETERNAL.get())) {
                bfs$wakePending = true; // food appeared/changed: rescan next tick
            }
        };
        handler.addListener(listener);
    }

    /** a free slot, or a non-eternal slot inside its same-food refresh window */
    @Unique
    private boolean bfs$hasFeedOpportunity(Player player) {
        if (Stomach.findEmptySlot(player) >= 0) {
            return true;
        }
        StomachData data = Stomach.get(player);
        int unlocked = Stomach.unlockedSlots(player);
        double refresh = Config.SAME_FOOD_REFRESH_TICKS.get();
        for (int i = 0; i < unlocked; i++) {
            StomachData.FoodSlot slot = data.slots[i];
            if (!slot.isEmpty() && !Double.isInfinite(slot.duration) && slot.duration <= refresh) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replaces SB's cooldowns (100 "idle", 10 "still hungry" — both meaningless under the
     * pinned hunger bar) with a stomach-aware cadence:
     *  - just fed with room left -> 40t burst (SB feeds ONE item per scan; bursts refill the rest)
     *  - everything else (stomach full, OR empty slots but nothing eligible) -> slow cadence
     *    (config, 580: a refresh always lands inside the 600t window). The wakes above turn the
     *    common cases (food added, slot freed) into a next-tick scan, so this is just the backstop.
     * The original constant tells us the outcome: SB passes 10 only after a successful feed.
     */
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "setCooldown(Lnet/minecraft/world/level/Level;I)V"), index = 1)
    private int bfs$scanCadence(int original) {
        Player player = bfs$tickPlayer.get();
        if (player != null && original == 10 && Stomach.findEmptySlot(player) >= 0) {
            return 40;
        }
        return Config.SB_FEEDING_SCAN_TICKS.get();
    }
}
