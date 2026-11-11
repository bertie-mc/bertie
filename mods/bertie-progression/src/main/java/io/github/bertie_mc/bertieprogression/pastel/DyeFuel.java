package io.github.bertie_mc.bertieprogression.pastel;

import io.github.bertie_mc.bertieprogression.ModAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Dye payments retain fractional charges without changing or duplicating dye stacks. */
public final class DyeFuel {
    public static final long POWER_PER_DYE = 100;

    private DyeFuel() {}

    public static long available(Player player, DyeColor color) {
        if (player.isCreative()) return Long.MAX_VALUE;
        if (color == null) return 0;
        long total = player.getData(ModAttachments.PASTEL_DYE_FUEL).getLong(color.getName());
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof DyeItem dye && dye.getDyeColor() == color) {
                total += stack.getCount() * POWER_PER_DYE;
            }
        }
        return total;
    }

    public static boolean drain(Player player, DyeColor color, long amount) {
        if (amount <= 0 || player.isCreative()) return true;
        if (color == null || available(player, color) < amount) return false;
        if (player.level().isClientSide) return true;
        CompoundTag credits = player.getData(ModAttachments.PASTEL_DYE_FUEL).copy();
        long balance = credits.getLong(color.getName());
        long toConsume = balance >= amount ? 0 : (amount - balance + POWER_PER_DYE - 1) / POWER_PER_DYE;
        long purchased = toConsume;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && toConsume > 0; slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof DyeItem dye && dye.getDyeColor() == color) {
                int count = (int) Math.min(toConsume, stack.getCount());
                stack.shrink(count);
                toConsume -= count;
            }
        }
        credits.putLong(color.getName(), balance + purchased * POWER_PER_DYE - amount);
        player.setData(ModAttachments.PASTEL_DYE_FUEL, credits);
        player.getInventory().setChanged();
        sync(player);
        return true;
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new DyeFuelPayload(
                            player.getData(ModAttachments.PASTEL_DYE_FUEL).copy()));
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.getEntity());
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        sync(event.getEntity());
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.getEntity());
    }
}
