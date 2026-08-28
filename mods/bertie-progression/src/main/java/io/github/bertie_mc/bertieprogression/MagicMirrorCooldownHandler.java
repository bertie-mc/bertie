package io.github.bertie_mc.bertieprogression;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * An hour between Magic Mirror trips.
 *
 * <p>TerraCurio's mirror teleports you to your spawn point for free and puts a ten tick cooldown on
 * itself, which is a use-animation guard rather than a cost. At that price it deletes every return
 * journey in the pack, so the cooldown becomes an hour of real time.
 *
 * <p>The Cell Phone is a PDA welded to a mirror and is removed from the pack, but removal is not
 * retroactive - one already sitting in a chest still works - so it is on the list too. Both ids are
 * looked up rather than imported: this mod does not depend on TerraCurio, and a missing item simply
 * drops out of the list.
 *
 * <p>Hooked on {@link LivingEntityUseItemEvent.Finish}, which NeoForge fires immediately after
 * {@code finishUsingItem} returns. That ordering is what makes this work at all - the mirror sets
 * its own ten ticks inside that call, and the hour written here replaces it.
 */
public final class MagicMirrorCooldownHandler {

    /** One hour of real time. */
    public static final int COOLDOWN_TICKS = 60 * 60 * 20;

    private static final List<ResourceLocation> MIRRORS = List.of(
            ResourceLocation.parse("terra_curio:magic_mirror"),
            ResourceLocation.parse("terra_curio:cell_phone"));

    private MagicMirrorCooldownHandler() {
    }

    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Item used = event.getItem().getItem();
        if (!MIRRORS.contains(BuiltInRegistries.ITEM.getKey(used))) {
            return;
        }
        player.getCooldowns().addCooldown(used, COOLDOWN_TICKS);
    }
}
