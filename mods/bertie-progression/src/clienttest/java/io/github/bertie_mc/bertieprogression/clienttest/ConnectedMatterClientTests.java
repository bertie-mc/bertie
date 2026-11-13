package io.github.bertie_mc.bertieprogression.clienttest;

import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import io.github.bertie_mc.testing.client.context.IntegratedWorldContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class ConnectedMatterClientTests {
    @ClientTest
    public static void resourceNamesRejectBiomeTagsAsElementIds(ClientTestContext context) {
        context.runOnClient(client -> {
            if (net.minecraft.resources.ResourceLocation.tryParse("#minecraft:is_overworld") != null) {
                throw new AssertionError("A biome tag must not decode as an element ID");
            }
            if (!net.minecraft.resources.ResourceLocation.parse("minecraft:plains")
                    .getPath()
                    .equals("plains")) {
                throw new AssertionError("Ordinary biome IDs must remain valid");
            }
        });
    }

    @ClientTest
    public static void captureConnections(ClientTestContext context) {
        context.runOnClient(client -> {
            for (String mod :
                    java.util.List.of("grandsigil", "archaion", "diorama", "weatherglass", "aaa_particles", "sodium")) {
                if (!net.neoforged.fml.ModList.get().isLoaded(mod))
                    throw new AssertionError("Missing client fixture mod " + mod);
            }
        });
        try (IntegratedWorldContext world = context.worldBuilder()
                .adjustSettings(settings -> {
                    settings.setName("Connected Negative Matter visual test");
                    settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    settings.setAllowCommands(true);
                })
                .create()) {
            world.server().runCommand("time set noon");
            world.server().runCommand("weather clear");
            world.server().runCommand("gamerule sendCommandFeedback false");
            world.server().runCommand("gamerule doMobSpawning false");
            world.server().runCommand("gamemode spectator @a");
            world.server().runCommand("fill -12 -60 -12 24 -45 12 minecraft:air");
            world.server().runCommand("fill -12 -61 -12 24 -61 12 minecraft:grass_block");
            world.server().runCommand("fill 0 -60 0 1 -60 1 anvilcraft:negative_matter_block");
            world.server().runCommand("fill 5 -60 0 6 -59 1 anvilcraft:negative_matter_block");
            world.server().runCommand("setblock 10 -60 0 anvilcraft:negative_matter_block");
            world.server().runCommand("fill 15 -60 0 17 -60 0 anvilcraft:negative_matter_block");
            world.server().runCommand("setblock 15 -60 1 anvilcraft:negative_matter_block");
            context.runOnClient(client -> {
                client.options.hideGui = true;
                client.options.pauseOnLostFocus = false;
                client.options.renderDistance().set(5);
                client.options.fov().set(32);
            });
            context.waitTicks(3);
            world.connection().waitForClientboundPackets();
            assertQuads(context, new BlockPos(0, -60, 0), 8);
            assertQuads(context, new BlockPos(5, -60, 0), 6);
            assertQuads(context, new BlockPos(10, -60, 0), 12);
            assertFloorJoin(context, new BlockPos(0, -60, 0), new BlockPos(1, -60, 0), 0);
            assertFloorJoin(context, new BlockPos(0, -60, 0), new BlockPos(0, -60, 1), 2);
            camera(context, world, "4.0 -59.2 4.3 139.85 23.3");
            context.takeScreenshot("negative-matter-connected-2x2");
            camera(context, world, "-3.6 -58.8 -3.5 -43.48 30.0");
            context.takeScreenshot("negative-matter-connected-opposite");
            camera(context, world, "9.9 -57.0 5.6 139.28 30.35");
            context.takeScreenshot("negative-matter-connected-2x2x2");
            camera(context, world, "12.8 -59 3.4 141.58 29.8");
            context.takeScreenshot("negative-matter-connected-single");
            camera(context, world, "20.8 -58.3 5.5 139.0 28.0");
            context.takeScreenshot("negative-matter-connected-l-shape");
            world.server().runCommand("setblock 1 -60 1 minecraft:air");
            context.waitTicks(3);
            world.connection().waitForClientboundPackets();
            assertQuads(context, new BlockPos(1, -60, 0), 10);
            camera(context, world, "4.0 -59.2 4.3 139.85 23.3");
            context.takeScreenshot("negative-matter-connected-after-removal");
        }
    }

    private static void assertQuads(ClientTestContext context, BlockPos pos, int expected) {
        context.runOnClient(client -> {
            var state = client.level.getBlockState(pos);
            var model = client.getBlockRenderer().getBlockModel(state);
            var data = model.getModelData(client.level, pos, state, ModelData.EMPTY);
            int count = model.getQuads(state, null, RandomSource.create(0), data, null)
                    .size();
            if (count != expected)
                throw new AssertionError(pos + ": expected " + expected + " quads, got " + count + " from "
                        + model.getClass().getName());
        });
    }

    @ClientTest
    public static void mapAndWeatherGlassRenderInTheConnectedWorld(ClientTestContext context) {
        try (IntegratedWorldContext world = context.worldBuilder()
                .adjustSettings(settings -> {
                    settings.setName("Map and Weather Glass integration");
                    settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    settings.setAllowCommands(true);
                })
                .create()) {
            world.server().runCommand("gamerule sendCommandFeedback false");
            world.server().runCommand("fill -8 64 -8 8 72 8 minecraft:air");
            world.server().runCommand("fill -8 63 -8 8 63 8 minecraft:grass_block");
            world.server().runCommand("fill -3 64 2 3 68 2 minecraft:glass");
            world.server().runCommand("tp @a 0.5 64 -3 0 0");
            world.server().runCommand("time set midnight");
            world.server().runCommand("weather rain 600");
            context.runOnClient(client -> {
                client.options.hideGui = false;
                client.options.pauseOnLostFocus = false;
                client.options.fov().set(70);
            });
            world.connection().waitForClientboundPackets();
            world.connection().waitForChunksRender(false);
            context.waitTicks(120);
            context.takeScreenshot("graphics-additions-glass-map");
        }
    }

    private static void assertFloorJoin(ClientTestContext context, BlockPos left, BlockPos right, int axis) {
        context.runOnClient(client -> {
            java.util.List<float[]> a = new java.util.ArrayList<>(), b = new java.util.ArrayList<>();
            for (BlockPos pos : new BlockPos[] {left, right}) {
                var state = client.level.getBlockState(pos);
                var model = client.getBlockRenderer().getBlockModel(state);
                var data = model.getModelData(client.level, pos, state, ModelData.EMPTY);
                for (var quad : model.getQuads(state, null, RandomSource.create(0), data, null)) {
                    if (!quad.getSprite().contents().name().getPath().equals("block/negative_matter_block")) continue;
                    var sprite = quad.getSprite();
                    int[] vertices = quad.getVertices();
                    int stride = vertices.length / 4;
                    for (int i = 0; i < 4; i++) {
                        int start = i * stride;
                        float u = Float.intBitsToFloat(vertices[start + 4]);
                        float v = Float.intBitsToFloat(vertices[start + 5]);
                        if (u < sprite.getU(1.5f / 16) - 1e-6
                                || u > sprite.getU(13.5f / 16) + 1e-6
                                || v < sprite.getV(1.5f / 16) - 1e-6
                                || v > sprite.getV(13.5f / 16) + 1e-6) {
                            throw new AssertionError("A connected surface samples the texture bevel");
                        }
                        float coord = Float.intBitsToFloat(vertices[start + axis]);
                        if (quad.getDirection() == Direction.UP && coord == (pos == left ? 1 : 0)) {
                            float x = Float.intBitsToFloat(vertices[start]) + pos.getX();
                            float y = Float.intBitsToFloat(vertices[start + 1]) + pos.getY();
                            float z = Float.intBitsToFloat(vertices[start + 2]) + pos.getZ();
                            (pos == left ? a : b).add(new float[] {x, y, z, u, v});
                        }
                    }
                }
            }
            if (a.size() != 2 || b.size() != 2) throw new AssertionError("Floor does not reach the join");
            for (float[] vertex : a) {
                boolean matches = b.stream().anyMatch(other -> {
                    for (int i = 0; i < vertex.length; i++) if (Math.abs(vertex[i] - other[i]) > 1e-6) return false;
                    return true;
                });
                if (!matches) throw new AssertionError("Floor geometry or UVs break at the join");
            }
        });
    }

    private static void camera(ClientTestContext context, IntegratedWorldContext world, String position) {
        world.server().runCommand("tp @a " + position);
        context.waitTick();
        world.connection().waitForClientboundPackets();
        world.connection().waitForChunksRender(false);
        context.waitTicks(10);
    }
}
