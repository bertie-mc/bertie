package com.berlord.explodetomine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = ExplodeToMine.MOD_ID)
public final class OreEvents {

    // Vanilla divides dig progress by 30 with the correct tool, by 100 with an incorrect tool.
    // Multiplying by 30/100 = 0.3 makes a locked ore feel exactly like mining with the wrong tool.
    private static final float WRONG_TOOL_FACTOR = 0.3F;

    /**
     * Explosion converts a locked ore into its cracked, mineable twin instead of destroying it.
     */
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        List<BlockPos> toCrack = new ArrayList<>();
        Iterator<BlockPos> it = event.getAffectedBlocks().iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (LockedOres.isLocked(level.getBlockState(pos))) {
                toCrack.add(pos.immutable());
                it.remove(); // spare it from the explosion
            }
        }

        for (BlockPos pos : toCrack) {
            Block twin = LockedOres.crackedTwinFor(level.getBlockState(pos));
            if (twin != null) {
                level.setBlockAndUpdate(pos, twin.defaultBlockState());
            }
        }
    }

    /**
     * Make locked ores slow to break by hand (same penalty as using the wrong tool),
     * so players don't accidentally chew through them.
     */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (LockedOres.isLocked(event.getState())) {
            event.setNewSpeed(event.getOriginalSpeed() * WRONG_TOOL_FACTOR);
        }
    }

    /**
     * Hand-mining a locked ore yields nothing: no item drops and no XP.
     */
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (LockedOres.isLocked(event.getState())) {
            event.getDrops().clear();
            event.setDroppedExperience(0);
        }
    }

    private OreEvents() {}
}
