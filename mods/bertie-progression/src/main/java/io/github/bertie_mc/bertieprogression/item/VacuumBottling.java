package io.github.bertie_mc.bertieprogression.item;

import io.github.bertie_mc.bertieprogression.ModItems;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class VacuumBottling {
    private static final Method AIRLESS;
    private static final Method ZONES;
    private static final Method COVERS;
    private static final Class<?> SEALED_CABIN;

    static {
        try {
            AIRLESS = Class.forName("me.moltea.unbound.life.Air").getMethod("airless", Level.class);
            var zones = Class.forName("me.moltea.unbound.life.AirZones");
            ZONES = zones.getMethod("of", ServerLevel.class);
            COVERS = zones.getMethod("covers", ServerLevel.class, Vec3.class);
            SEALED_CABIN = Class.forName("me.moltea.unbound.entity.SealedCabin");
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static boolean exposed(ServerPlayer player) {
        if (player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)
                || player.isInLava()
                || SEALED_CABIN.isInstance(player.getVehicle())) return false;
        try {
            var level = player.serverLevel();
            return (boolean) AIRLESS.invoke(null, level)
                    && !(boolean) COVERS.invoke(ZONES.invoke(null, level), level, player.getEyePosition());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot inspect Unbound vacuum", failure);
        }
    }

    @SubscribeEvent
    public static void collect(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.isSpectator()
                || !event.getItemStack().is(Items.GLASS_BOTTLE)
                || !exposed(player)) return;
        player.setItemInHand(
                event.getHand(),
                ItemUtils.createFilledResult(
                        event.getItemStack(), player, new ItemStack(ModItems.BOTTLE_OF_NOTHING.get())));
        player.awardStat(Stats.ITEM_USED.get(Items.GLASS_BOTTLE));
        player.serverLevel()
                .playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.6f, 1.15f);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private VacuumBottling() {}
}
