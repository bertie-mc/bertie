package io.github.bertie_mc.bertieprogression.compat;

import java.lang.reflect.Method;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Resolves Forgified Fabric API's placeholder through its phase/direction registry. */
public final class FabricPayloadCodecs {
    private record Binding(Object placeholder, Method registry, Method payload) {}

    private static final class Holder {
        private static final Binding BINDING = discover();
    }

    private static Binding discover() {
        try {
            Class<?> registrar = Class.forName("org.sinytra.fabric.networking_api.NeoNetworkRegistrar");
            Class<?> registry = Class.forName("net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl");
            return new Binding(
                    registrar.getField("DUMMY_CODEC").get(null),
                    registrar.getMethod("getPayloadRegistry", ConnectionProtocol.class, PacketFlow.class),
                    registry.getMethod("get", ResourceLocation.class));
        } catch (ClassNotFoundException absent) {
            return null;
        } catch (ReflectiveOperationException incompatible) {
            throw new IllegalStateException("Cannot access the installed Fabric payload registry", incompatible);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload> resolve(
            StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload> original,
            ResourceLocation id,
            ConnectionProtocol protocol,
            PacketFlow flow) {
        Binding binding = Holder.BINDING;
        if (binding == null
                || original != binding.placeholder()
                || protocol != ConnectionProtocol.PLAY && protocol != ConnectionProtocol.CONFIGURATION) return original;
        try {
            Object registry = binding.registry().invoke(null, protocol, flow);
            Object found = binding.payload().invoke(registry, id);
            return found instanceof CustomPacketPayload.TypeAndCodec<?, ?> codec
                    ? (StreamCodec) codec.codec()
                    : original;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot resolve Fabric payload codec for " + id, failure);
        }
    }

    private FabricPayloadCodecs() {}
}
