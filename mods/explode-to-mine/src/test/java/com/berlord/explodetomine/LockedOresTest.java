package com.berlord.explodetomine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
class LockedOresTest {
    @Test
    void diamondOresResolveToTheirCrackedTwins(MinecraftServer server) {
        assertTrue(LockedOres.isLocked(Blocks.DIAMOND_ORE.defaultBlockState()));
        assertSame(ModBlocks.CRACKED_DIAMOND_ORE.get(),
                LockedOres.crackedTwinFor(Blocks.DIAMOND_ORE.defaultBlockState()));

        assertTrue(LockedOres.isLocked(Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState()));
        assertSame(ModBlocks.CRACKED_DEEPSLATE_DIAMOND_ORE.get(),
                LockedOres.crackedTwinFor(Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState()));
    }

    @Test
    void ordinaryOresRemainUnaffected(MinecraftServer server) {
        assertFalse(LockedOres.isLocked(Blocks.IRON_ORE.defaultBlockState()));
        assertNull(LockedOres.crackedTwinFor(Blocks.IRON_ORE.defaultBlockState()));
    }
}
