package io.github.bertie_mc.bertieprogression.clienttest;

import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import io.github.bertie_mc.testing.client.context.IntegratedWorldContext;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class ConnectedMatterCornerClientTests {
    private static final float R = 0.5f / 16;

    @ClientTest
    public static void verifyEveryCorner(ClientTestContext context) {
        try (IntegratedWorldContext world = context.worldBuilder()
                .adjustSettings(settings -> {
                    settings.setName("Negative Matter corner continuity");
                    settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    settings.setAllowCommands(true);
                })
                .create()) {
            world.server().runCommand("save-off");
            world.server().runCommand("time set noon");
            world.server().runCommand("weather clear");
            world.server().runCommand("gamerule sendCommandFeedback false");
            world.server().runCommand("gamerule doMobSpawning false");
            world.server().runCommand("gamemode spectator @a");
            world.server().runCommand("fill -14 -61 -14 24 -61 12 minecraft:grass_block");
            world.server().runCommand("fill 0 -60 0 1 -60 1 anvilcraft:negative_matter_block");
            world.server().runCommand("fill 5 -60 0 6 -59 1 anvilcraft:negative_matter_block");
            world.server().runCommand("fill 15 -60 0 17 -60 0 anvilcraft:negative_matter_block");
            world.server().runCommand("setblock 15 -60 1 anvilcraft:negative_matter_block");
            world.server().runCommand("fill 10 -54 0 11 -53 1 anvilcraft:negative_matter_block");
            world.server().runCommand("fill 10 -60 6 12 -58 6 anvilcraft:negative_matter_block");
            world.server().runCommand("fill 10 -60 7 10 -58 7 anvilcraft:negative_matter_block");
            world.server().runCommand("fill 5 -58 8 6 -58 8 anvilcraft:negative_matter_block");
            world.server().runCommand("setblock 6 -59 8 anvilcraft:negative_matter_block");
            world.server().runCommand("fill 0 -57 8 1 -56 9 anvilcraft:negative_matter_block");
            world.server().runCommand("setblock 1 -56 9 minecraft:air");
            for (int x = 0; x < 2; x++)
                for (int y = 0; y < 2; y++)
                    for (int z = 0; z < 2; z++) {
                        world.server()
                                .runCommand("setblock " + (-12 + 3 * x) + " " + (-57 + 3 * y) + " " + (-12 + 3 * z)
                                        + " anvilcraft:negative_matter_block");
                    }
            context.runOnClient(client -> {
                client.options.hideGui = true;
                client.options.pauseOnLostFocus = false;
                client.options.renderDistance().set(5);
                client.options.fov().set(32);
            });
            context.waitTicks(5);
            world.connection().waitForClientboundPackets();
            context.runOnClient(client -> {
                for (int x = 0; x < 2; x++)
                    for (int y = 0; y < 2; y++)
                        for (int z = 0; z < 2; z++) {
                            verifyCubeCorners(client, new BlockPos(-12 + 3 * x, -57 + 3 * y, -12 + 3 * z));
                        }
                BlockPos first = new BlockPos(0, -60, 0);
                for (float t : new float[] {0.1f, 0.25f, 0.5f, 0.75f, 0.9f}) {
                    same(
                            client,
                            first,
                            Direction.UP,
                            new float[] {1, R, t},
                            new BlockPos(1, -60, 0),
                            Direction.UP,
                            new float[] {0, R, t},
                            "floor X join");
                    same(
                            client,
                            first,
                            Direction.UP,
                            new float[] {t, R, 1},
                            new BlockPos(0, -60, 1),
                            Direction.UP,
                            new float[] {t, R, 0},
                            "floor Z join");
                    same(
                            client,
                            new BlockPos(16, -60, 0),
                            Direction.NORTH,
                            new float[] {-R, t, 1 - R},
                            new BlockPos(15, -60, 1),
                            Direction.WEST,
                            new float[] {1 - R, t, -R},
                            "concave L join");
                    same(
                            client,
                            new BlockPos(11, -59, 6),
                            Direction.NORTH,
                            new float[] {-R, t, 1 - R},
                            new BlockPos(10, -59, 7),
                            Direction.WEST,
                            new float[] {1 - R, t, -R},
                            "tall concave join");
                    same(
                            client,
                            new BlockPos(5, -58, 8),
                            Direction.UP,
                            new float[] {1 + R, R, t},
                            new BlockPos(6, -59, 8),
                            Direction.EAST,
                            new float[] {R, 1 + R, t},
                            "stepped concave join");
                }
            });
            camera(context, world, "1 -54 12 180 30");
            for (int attempt = 0; attempt < 100; attempt++) {
                if (context.computeOnClient(client ->
                        !client.level.getBlockState(new BlockPos(0, -56, 9)).isAir())) break;
                context.waitTicks(2);
            }
            context.runOnClient(client -> {
                same(
                        client,
                        new BlockPos(0, -56, 9),
                        Direction.WEST,
                        new float[] {1 - R, -R, -R},
                        new BlockPos(1, -57, 9),
                        Direction.DOWN,
                        new float[] {-R, 1 - R, -R},
                        "three-face concave corner XY");
                same(
                        client,
                        new BlockPos(0, -56, 9),
                        Direction.WEST,
                        new float[] {1 - R, -R, -R},
                        new BlockPos(1, -56, 8),
                        Direction.NORTH,
                        new float[] {-R, -R, 1 - R},
                        "three-face concave corner XZ");
            });
            camera(context, world, "4.0 -59.2 4.3 139.85 23.3");
            context.takeScreenshot("negative-matter-corners-flat");
            String[] positions = {
                "15 -51.1 5 135 31.9",
                "7 -51.1 5 -135 31.9",
                "7 -51.1 -3 -45 31.9",
                "15 -51.1 -3 45 31.9",
                "15 -58.14 5 135 -31.9",
                "7 -58.14 5 -135 -31.9",
                "7 -58.14 -3 -45 -31.9",
                "15 -58.14 -3 45 -31.9"
            };
            String[] names = {
                "above-se", "above-sw", "above-nw", "above-ne", "below-se", "below-sw", "below-nw", "below-ne"
            };
            for (int i = 0; i < positions.length; i++) {
                camera(context, world, positions[i]);
                context.takeScreenshot("negative-matter-corners-" + names[i]);
            }
            camera(context, world, "20.8 -58.3 5.5 139.0 28.0");
            context.takeScreenshot("negative-matter-corners-l-shape");
            camera(context, world, "15 -57.8 11 142 22");
            context.takeScreenshot("negative-matter-corners-concave-tall");
            camera(context, world, "9 -60.2 11.5 139 -12");
            context.takeScreenshot("negative-matter-corners-step");
            camera(context, world, "5 -54 13 135 30");
            context.takeScreenshot("negative-matter-corners-three-face");
            world.server().runCommand("setblock 15 -60 1 minecraft:air");
            context.waitTicks(5);
            world.connection().waitForClientboundPackets();
            context.runOnClient(client -> {
                // Removing the diagonal block must withdraw the formerly extended face.
                for (var q : surfaces(client, new BlockPos(16, -60, 0))) {
                    if (q.getDirection() != Direction.NORTH) continue;
                    int[] v = q.getVertices();
                    for (int i = 0; i < 4; i++)
                        if (Float.intBitsToFloat(v[i * 8]) < -1e-6) {
                            throw new AssertionError("Concave extension remained after removing its diagonal neighbor");
                        }
                }
            });
            camera(context, world, "20.8 -58.3 5.5 139.0 28.0");
            context.takeScreenshot("negative-matter-corners-after-removal");
            camera(context, world, "4.0 -59.2 4.3 139.85 23.3");
            context.waitTicks(20);
        }
    }

    static void assertFlatJoin(ClientTestContext context, BlockPos left, BlockPos right, int axis) {
        context.runOnClient(client -> {
            for (float t : new float[] {0.1f, 0.25f, 0.5f, 0.75f, 0.9f}) {
                float[] a = axis == 0 ? new float[] {1, R, t} : new float[] {t, R, 1};
                float[] b = axis == 0 ? new float[] {0, R, t} : new float[] {t, R, 0};
                same(client, left, Direction.UP, a, right, Direction.UP, b, "coplanar floor join");
            }
        });
    }

    private static List<BakedQuad> surfaces(Minecraft client, BlockPos pos) {
        var state = client.level.getBlockState(pos);
        var model = client.getBlockRenderer().getBlockModel(state);
        var data = model.getModelData(client.level, pos, state, ModelData.EMPTY);
        return model.getQuads(state, null, RandomSource.create(0), data, null).stream()
                .filter(q -> q.getSprite().contents().name().getPath().equals("block/negative_matter_block"))
                .toList();
    }

    private static void verifyCubeCorners(Minecraft client, BlockPos pos) {
        Direction[] negative = {Direction.EAST, Direction.UP, Direction.SOUTH};
        Direction[] positive = {Direction.WEST, Direction.DOWN, Direction.NORTH};
        for (int a = 0; a < 3; a++)
            for (int b = a + 1; b < 3; b++)
                for (int sa = 0; sa < 2; sa++)
                    for (int sb = 0; sb < 2; sb++) {
                        for (float t : new float[] {0.1f, 0.33f, 0.5f, 0.66f, 0.9f}) {
                            float[] point = new float[3];
                            point[a] = sa == 0 ? R : 1 - R;
                            point[b] = sb == 0 ? R : 1 - R;
                            point[3 - a - b] = t;
                            same(
                                    client,
                                    pos,
                                    sa == 0 ? negative[a] : positive[a],
                                    point,
                                    pos,
                                    sb == 0 ? negative[b] : positive[b],
                                    point,
                                    "cube edge at " + pos);
                        }
                    }
    }

    private static void same(
            Minecraft client,
            BlockPos a,
            Direction faceA,
            float[] pointA,
            BlockPos b,
            Direction faceB,
            float[] pointB,
            String label) {
        float[] uvA =
                sample(surfaces(client, a), faceA, pointA, label + " at " + a + " " + client.level.getBlockState(a));
        float[] uvB =
                sample(surfaces(client, b), faceB, pointB, label + " at " + b + " " + client.level.getBlockState(b));
        if (Math.abs(uvA[0] - uvB[0]) > 2e-5 || Math.abs(uvA[1] - uvB[1]) > 2e-5) {
            throw new AssertionError(label + ": texture breaks between " + faceA + " and " + faceB);
        }
    }

    private static float[] sample(List<BakedQuad> quads, Direction face, float[] point, String label) {
        int normal = face.getAxis().ordinal(), axisA = (normal + 1) % 3, axisB = (normal + 2) % 3;
        for (var quad : quads) {
            if (quad.getDirection() != face) continue;
            int[] v = quad.getVertices();
            for (int[] triangle : new int[][] {{0, 1, 2}, {0, 2, 3}}) {
                int a = triangle[0] * 8, b = triangle[1] * 8, c = triangle[2] * 8;
                float ax = Float.intBitsToFloat(v[a + axisA]), ay = Float.intBitsToFloat(v[a + axisB]);
                float bx = Float.intBitsToFloat(v[b + axisA]), by = Float.intBitsToFloat(v[b + axisB]);
                float cx = Float.intBitsToFloat(v[c + axisA]), cy = Float.intBitsToFloat(v[c + axisB]);
                float determinant = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
                if (Math.abs(determinant) < 1e-9) continue;
                float weightB = ((point[axisA] - ax) * (cy - ay) - (point[axisB] - ay) * (cx - ax)) / determinant;
                float weightC = ((bx - ax) * (point[axisB] - ay) - (by - ay) * (point[axisA] - ax)) / determinant;
                float weightA = 1 - weightB - weightC;
                if (weightA < -1e-5 || weightB < -1e-5 || weightC < -1e-5) continue;
                if (Math.abs(Float.intBitsToFloat(v[a + normal]) - point[normal]) > 1e-5) continue;
                return new float[] {
                    weightA * Float.intBitsToFloat(v[a + 4])
                            + weightB * Float.intBitsToFloat(v[b + 4])
                            + weightC * Float.intBitsToFloat(v[c + 4]),
                    weightA * Float.intBitsToFloat(v[a + 5])
                            + weightB * Float.intBitsToFloat(v[b + 5])
                            + weightC * Float.intBitsToFloat(v[c + 5])
                };
            }
        }
        StringBuilder geometry = new StringBuilder();
        for (var q : quads)
            if (q.getDirection() == face) {
                geometry.append(" [");
                for (int i = 0; i < 4; i++) {
                    int[] v = q.getVertices();
                    geometry.append(Float.intBitsToFloat(v[i * 8]))
                            .append(',')
                            .append(Float.intBitsToFloat(v[i * 8 + 1]))
                            .append(',')
                            .append(Float.intBitsToFloat(v[i * 8 + 2]))
                            .append(';');
                }
                geometry.append(']');
            }
        throw new AssertionError(label + ": missing surface at " + java.util.Arrays.toString(point) + " facing " + face
                + " quads=" + quads.size() + geometry);
    }

    private static void camera(ClientTestContext context, IntegratedWorldContext world, String position) {
        world.server().runCommand("tp @a " + position);
        context.waitTick();
        world.connection().waitForClientboundPackets();
        world.connection().waitForChunksRender(false);
        context.waitTicks(10);
    }
}
