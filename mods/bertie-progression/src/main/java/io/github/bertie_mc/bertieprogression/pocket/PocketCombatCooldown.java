package io.github.bertie_mc.bertieprogression.pocket;

import io.github.bertie_mc.bertieprogression.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * The Pocket Dimension cannot be entered for ten seconds after its user takes a hit, whether they
 * crouch on the block or press the Open Portal key. A refused attempt shows the time left in the
 * middle of the screen.
 */
public final class PocketCombatCooldown {
    static final long COOLDOWN_TICKS = 200;
    private static final long NOTICE_INTERVAL_TICKS = 10;

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getOriginalDamage() > 0) {
            player.setData(ModAttachments.LAST_HURT_TIME, player.serverLevel().getGameTime());
        }
    }

    /** Whether the player may enter now; when not, the remaining time is shown to them. */
    public static boolean allowEntry(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        long lastHurt = player.getData(ModAttachments.LAST_HURT_TIME);
        long remaining = lastHurt == Long.MIN_VALUE ? 0 : lastHurt + COOLDOWN_TICKS - now;
        if (remaining <= 0) {
            return true;
        }
        long lastNotice = player.getData(ModAttachments.COOLDOWN_NOTICE_TIME);
        if (lastNotice == Long.MIN_VALUE || now - lastNotice >= NOTICE_INTERVAL_TICKS || now < lastNotice) {
            player.setData(ModAttachments.COOLDOWN_NOTICE_TIME, now);
            long seconds = (remaining + 19) / 20;
            player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 30, 10));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
            player.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.translatable("message.bertieprogression.pocket_combat_cooldown", seconds)));
        }
        return false;
    }

    private PocketCombatCooldown() {}
}
