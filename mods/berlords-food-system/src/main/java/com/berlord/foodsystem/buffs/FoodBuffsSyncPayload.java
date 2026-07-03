package com.berlord.foodsystem.buffs;

import com.berlord.foodsystem.BFS;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Raw food-buffs JSON, server -> client, so climbing prediction matches the server. */
public record FoodBuffsSyncPayload(String json) implements CustomPacketPayload {

    public static final Type<FoodBuffsSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BFS.MODID, "food_buffs_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FoodBuffsSyncPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.stringUtf8(1 << 20), FoodBuffsSyncPayload::json, FoodBuffsSyncPayload::new);

    @Override
    public Type<FoodBuffsSyncPayload> type() {
        return TYPE;
    }

    public static void handle(FoodBuffsSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> BuffsConfig.applyJson(payload.json()));
    }
}
