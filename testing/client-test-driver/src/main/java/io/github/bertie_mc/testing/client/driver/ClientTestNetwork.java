package io.github.bertie_mc.testing.client.driver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

final class ClientTestNetwork {
    private static final AtomicLong NEXT_BARRIER = new AtomicLong();
    private static final ConcurrentMap<Long, CompletableFuture<Void>> BARRIERS =
            new ConcurrentHashMap<>();

    private ClientTestNetwork() {}

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playBidirectional(
                SyncPayload.TYPE,
                SyncPayload.STREAM_CODEC,
                (payload, context) -> complete(payload.id()));
    }

    static Barrier newBarrier() {
        long id = NEXT_BARRIER.incrementAndGet();
        CompletableFuture<Void> completion = new CompletableFuture<>();
        BARRIERS.put(id, completion);
        return new Barrier(new SyncPayload(id), completion);
    }

    static void discard(Barrier barrier) {
        BARRIERS.remove(barrier.payload.id(), barrier.completion);
    }

    private static void complete(long id) {
        CompletableFuture<Void> barrier = BARRIERS.get(id);
        if (barrier != null) {
            barrier.complete(null);
        }
    }

    record Barrier(SyncPayload payload, CompletableFuture<Void> completion) {}

    record SyncPayload(long id) implements CustomPacketPayload {
        private static final Type<SyncPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ClientTestDriver.MOD_ID, "packet_barrier"));
        private static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_LONG, SyncPayload::id, SyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
