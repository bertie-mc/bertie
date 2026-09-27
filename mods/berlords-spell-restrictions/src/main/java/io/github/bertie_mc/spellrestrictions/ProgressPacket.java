package io.github.bertie_mc.spellrestrictions;

import java.util.HashSet;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ProgressPacket(List<String> spells, int tier) implements CustomPacketPayload {
    public static final Type<ProgressPacket> TYPE = new Type<>(SpellRestrictions.id("progress"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressPacket> CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeCollection(packet.spells, (b, spell) -> b.writeUtf(spell));
                buffer.writeVarInt(packet.tier);
            },
            buffer -> new ProgressPacket(buffer.readList(b -> b.readUtf()), buffer.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ProgressPacket packet, IPayloadContext context) {
        context.player()
                .setData(
                        SpellRestrictions.PROGRESS,
                        new ProgressData(new UnlockState(new HashSet<>(packet.spells), packet.tier)));
    }

    public static void send(ServerPlayer player) {
        if (player.connection != null)
            PacketDistributor.sendToPlayer(
                    player,
                    new ProgressPacket(
                            List.copyOf(Restrictions.state(player).spells()),
                            Restrictions.state(player).tier()));
    }
}
