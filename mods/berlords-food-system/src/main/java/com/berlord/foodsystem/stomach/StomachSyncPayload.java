package com.berlord.foodsystem.stomach;

import com.berlord.foodsystem.BFS;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Full stomach snapshot, server -> owning client. */
public record StomachSyncPayload(CompoundTag nbt) implements CustomPacketPayload {

    public static final Type<StomachSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BFS.MODID, "stomach_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StomachSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeNbt(payload.nbt()),
            buf -> new StomachSyncPayload((CompoundTag) buf.readNbt()));

    @Override
    public Type<StomachSyncPayload> type() {
        return TYPE;
    }

    public static void handle(StomachSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.nbt() != null) {
                Stomach.get(context.player()).deserializeNBT(context.player().registryAccess(), payload.nbt());
            }
        });
    }
}
