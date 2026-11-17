package io.github.bertie_mc.bertieprogression.clienttest;

import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import java.util.List;
import mekanism.api.MekanismAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

public final class StellarisRetirementClientTests {
    @ClientTest
    public static void removedOreFormsStayAbsentAndSharedMetalsRemain(ClientTestContext context) throws Exception {
        try (var world = context.worldBuilder().create()) {
            world.server().runOnServer(server -> {
                if (ModList.get().isLoaded("stellaris"))
                    throw new AssertionError("Retirement fixture unexpectedly loads Stellaris");
                if (!ModList.get().isLoaded("bertieemi"))
                    throw new AssertionError("Optional EMI integration must be present in this fixture");
                var chemicals = server.registryAccess().registryOrThrow(MekanismAPI.CHEMICAL_REGISTRY_NAME);
                for (String material : List.of("desh", "neptunium", "tharsite", "corronium", "solerium")) {
                    for (String form : List.of(
                            "dust",
                            "dirty_dust",
                            "clump",
                            "gem",
                            "shard",
                            "crystal",
                            "spec",
                            "dense_charge",
                            "activated_crystal")) {
                        var id = ResourceLocation.parse("bertieprogression:" + material + "_" + form);
                        if (BuiltInRegistries.ITEM.containsKey(id))
                            throw new AssertionError("Removed material form still registered: " + id);
                    }
                    for (String state : List.of("dirty", "clean")) {
                        var id = ResourceLocation.parse("bertieprogression:" + state + "_" + material);
                        if (chemicals.containsKey(id))
                            throw new AssertionError("Removed material slurry still registered: " + id);
                    }
                }
                for (String id : List.of(
                        "anvilcraft:uranium_ingot",
                        "mekanism:pellet_plutonium",
                        "oritech:plutonium_dust",
                        "minecraft:iron_ingot",
                        "mekanism:ingot_steel")) {
                    if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id)))
                        throw new AssertionError("Unrelated metal was removed: " + id);
                }
            });
            world.connection().waitForClientboundPackets();
            context.waitTicks(20);
            context.runOnClient(client -> {
                if (client.level == null || client.player == null)
                    throw new AssertionError("Client failed to remain connected without Stellaris");
            });
        }
    }
}
