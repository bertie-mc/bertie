package io.github.bertie_mc.bertieprogression.clienttest;

import io.github.bertie_mc.bertieprogression.compat.FabricPayloadCodecs;
import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

public final class NetworkPayloadClientTests {
    @ClientTest
    public static void barrelRollConfigurationSyncAllowsWorldJoin(ClientTestContext context) throws Exception {
        try (var world = context.worldBuilder().create()) {
            world.server().runOnServer(server -> {
                var id = ResourceLocation.parse("do_a_barrel_roll:config_sync");
                var registrar = Class.forName("org.sinytra.fabric.networking_api.NeoNetworkRegistrar");
                var registryClass = Class.forName("net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl");
                var dummy = registrar.getField("DUMMY_CODEC").get(null);
                int registered = 0;
                for (var phase : java.util.List.of(ConnectionProtocol.PLAY, ConnectionProtocol.CONFIGURATION)) {
                    var registry = registrar
                            .getMethod("getPayloadRegistry", ConnectionProtocol.class, PacketFlow.class)
                            .invoke(null, phase, PacketFlow.CLIENTBOUND);
                    if (registryClass.getMethod("get", ResourceLocation.class).invoke(registry, id) == null) continue;
                    registered++;
                    var codec = NetworkRegistry.getCodec(id, phase, PacketFlow.CLIENTBOUND);
                    if (codec == null || codec == dummy)
                        throw new AssertionError("Fabric config sync still uses its placeholder codec");
                    if (FabricPayloadCodecs.resolve(codec, id, phase, PacketFlow.CLIENTBOUND) != codec)
                        throw new AssertionError("Already-resolved payload codec must be preserved");
                }
                if (registered == 0)
                    throw new AssertionError("The failing Do a Barrel Roll packet is absent from this fixture");
                if (server.getPlayerList().getPlayers().size() != 1)
                    throw new AssertionError("Client failed to join after configuration sync");
            });
            world.connection().waitForClientboundPackets();
            context.waitTicks(10);
            context.runOnClient(client -> {
                if (client.player == null || client.level == null)
                    throw new AssertionError("Client disconnected after Fabric packet sync");
            });
        }
    }
}
