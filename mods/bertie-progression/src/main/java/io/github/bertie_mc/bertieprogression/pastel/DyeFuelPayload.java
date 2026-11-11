package io.github.bertie_mc.bertieprogression.pastel;

import io.github.bertie_mc.bertieprogression.BertieProgression;
import io.github.bertie_mc.bertieprogression.ModAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DyeFuelPayload(CompoundTag credits) implements CustomPacketPayload {
    public static final Type<DyeFuelPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, "pastel_dye_fuel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DyeFuelPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> buf.writeNbt(payload.credits()), buf -> new DyeFuelPayload(buf.readNbt()));

    @Override
    public Type<DyeFuelPayload> type() {
        return TYPE;
    }

    private static void handle(DyeFuelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.credits() != null) {
                context.player()
                        .setData(
                                ModAttachments.PASTEL_DYE_FUEL,
                                payload.credits().copy());
            }
        });
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, DyeFuelPayload::handle);
    }
}
