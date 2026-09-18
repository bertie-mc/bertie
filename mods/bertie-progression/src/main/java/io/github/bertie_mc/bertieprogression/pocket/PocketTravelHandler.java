package io.github.bertie_mc.bertieprogression.pocket;

import io.github.bertie_mc.bertieprogression.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.pocketdimension.procedures.OpenKeyPressetProcedure;

/** Loaded only when Pocket Dimension is installed. Its original return portal stays intact. */
public final class PocketTravelHandler {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("pocket_dimension", "pocket_dimension"));

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.isAlive()
                || player.isSpectator()
                || !player.isShiftKeyDown()
                || !player.onGround()
                || player.level().dimension().equals(DIMENSION)
                || !player.level().getBlockState(player.getOnPos()).is(ModBlocks.POCKET_DIMENSION)) {
            return;
        }
        // This entry route does not use OpenKeyMessage, so it needs no watch unlock.
        // Upstream owns the cooldown, animation, plot allocation and saved return position.
        // Crouching is this block's trigger, not upstream's request to take nearby mobs along.
        player.setShiftKeyDown(false);
        try {
            OpenKeyPressetProcedure.execute(player.serverLevel(), player);
        } finally {
            player.setShiftKeyDown(true);
        }
    }

    private PocketTravelHandler() {}
}
