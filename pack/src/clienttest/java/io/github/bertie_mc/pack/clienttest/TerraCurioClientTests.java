package io.github.bertie_mc.pack.clienttest;

import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.confluence.terra_curio.mixed.ILivingEntity;
import top.theillusivec4.curios.api.CuriosApi;

public final class TerraCurioClientTests {
    private static final ResourceLocation TERRASPARK_BOOTS_ID =
            ResourceLocation.fromNamespaceAndPath("terra_curio", "terraspark_boots");

    private TerraCurioClientTests() {}

    @ClientTest
    public static void terrasparkBootsCreateParticleEmitter(ClientTestContext context) {
        Item terrasparkBoots = BuiltInRegistries.ITEM.get(TERRASPARK_BOOTS_ID);

        try (var server = context.worldBuilder().createServer()) {
            try (var connection = server.connect()) {
                server.runOnServer(ignored -> CuriosApi.getCuriosInventory(connection.serverPlayer())
                        .orElseThrow(() -> new AssertionError("Server player has no Curios inventory"))
                        .setEquippedCurio("accessory", 0, new ItemStack(terrasparkBoots)));

                context.waitFor(
                        "Terraspark Boots to synchronize",
                        ignored -> CuriosApi.getCuriosInventory(connection.clientPlayer())
                                .flatMap(inventory -> inventory.findFirstCurio(terrasparkBoots))
                                .isPresent());
                context.waitFor(
                        "Terra Curio to create a particle emitter",
                        ignored -> !((ILivingEntity) connection.clientPlayer())
                                .terra_curio$getOrCreateParticleEmitters()
                                .isEmpty());
            }
        }
    }
}
