package io.github.bertie_mc.witheringwaver;

import io.github.bertie_mc.witheringwaver.entity.WitheringWaverEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = WitheringWaver.MOD_ID)
public final class WwGameEvents {
    /** One Waver per this many fresh wither skeleton spawns. */
    private static final int REPLACE_ONE_IN = 30;

    private WwGameEvents() {}

    /**
     * The Waver duplicates wither skeleton spawn conditions by replacing 1 in 30 fresh
     * wither skeleton spawns in place. Summons, eggs, spawners and conversions are left
     * alone; {@code MOB_SUMMONED} in particular must stay excluded or the Waver's own
     * summon could recurse.
     */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof WitherSkeleton skeleton)) {
            return;
        }
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION && type != MobSpawnType.STRUCTURE) {
            return;
        }
        if (skeleton.getRandom().nextInt(REPLACE_ONE_IN) != 0) {
            return;
        }
        ServerLevelAccessor level = event.getLevel();
        WitheringWaverEntity waver = WwEntities.WITHERING_WAVER.get().create(level.getLevel());
        if (waver == null) {
            return;
        }
        waver.moveTo(event.getX(), event.getY(), event.getZ(), skeleton.getYRot(), 0.0F);
        EventHooks.finalizeMobSpawn(waver, level, event.getDifficulty(), MobSpawnType.CONVERSION, null);
        event.setSpawnCancelled(true);
        event.setCanceled(true);
        level.addFreshEntity(waver);
    }

    /**
     * The skeleton family and the Waver never hurt each other, in either direction —
     * that includes the Waver's own wither-skull explosions splashing back onto it or
     * onto its summons. The one exception is the Waver's reap/assimilate kill itself,
     * recognisable by the tag it puts on the victim just before striking.
     */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        var victim = event.getEntity();

        boolean fromWaver = attacker instanceof WitheringWaverEntity || direct instanceof WitheringWaverEntity;
        if (victim instanceof WitheringWaverEntity
                && ((attacker != null && WitheringWaverEntity.isSkeletonFamily(attacker))
                        || (direct != null && WitheringWaverEntity.isSkeletonFamily(direct)))) {
            event.setCanceled(true);
            return;
        }
        if (fromWaver
                && WitheringWaverEntity.isSkeletonFamily(victim)
                && !victim.getTags().contains(WitheringWaverEntity.TAG_REAPED)
                && !victim.getTags().contains(WitheringWaverEntity.TAG_ASSIMILATED)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var entity = event.getEntity();
        if (entity.getTags().contains(WitheringWaverEntity.TAG_SUMMONED)
                || entity.getTags().contains(WitheringWaverEntity.TAG_ASSIMILATED)) {
            event.setCanceled(true);
            return;
        }
        if (entity.getTags().contains(WitheringWaverEntity.TAG_REAPED)) {
            event.getDrops().removeIf(item -> item.getItem().is(Items.WITHER_SKELETON_SKULL));
        }
    }

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        var tags = event.getEntity().getTags();
        if (tags.contains(WitheringWaverEntity.TAG_SUMMONED) || tags.contains(WitheringWaverEntity.TAG_ASSIMILATED)) {
            event.setCanceled(true);
        }
    }
}
