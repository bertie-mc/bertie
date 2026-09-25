package io.github.bertie_mc.armorcompletions;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.bertie_mc.armorcompletions.client.models.BoneReptileBootsModel;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BootModelQualityTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void bootsHaveFullHeightAndEnoughOpaqueTexturePixelsForEveryFace() throws Exception {
        for (String family :
                List.of("bishop_of_deceit", "pyromancer_brute", "nameless_one", "necromancer", "bone_reptile")) {
            boolean nativeModel = family.equals("bone_reptile");
            String namespace = nativeModel ? "cataclysm" : "hazennstuff";
            String geometry = nativeModel
                    ? "/assets/armorcompletions/geometry/bone_reptile_boots.json"
                    : "/assets/hazennstuff/geo/armor/" + family + "_completed.geo.json";
            try (var input = getClass().getResourceAsStream(geometry);
                    var texture = getClass()
                            .getResourceAsStream(
                                    "/assets/" + namespace + "/textures/armor/" + family + "_completed.png")) {
                var data = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                JsonArray bones = nativeModel
                        ? data.getAsJsonArray("bones")
                        : data.getAsJsonArray("minecraft:geometry")
                                .get(0)
                                .getAsJsonObject()
                                .getAsJsonArray("bones");
                var image = ImageIO.read(texture);
                int checked = 0;
                for (var element : bones) {
                    var bone = element.getAsJsonObject();
                    if (!bone.get("name").getAsString().endsWith("Boot")) continue;
                    var cubes = bone.getAsJsonArray("cubes");
                    assertTrue(cubes.size() >= 9, family);
                    double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
                    for (var entry : cubes) {
                        var cube = entry.getAsJsonObject();
                        var size = cube.getAsJsonArray("size");
                        double w = size.get(0).getAsDouble(),
                                h = size.get(1).getAsDouble(),
                                d = size.get(2).getAsDouble();
                        assertTrue(w >= 1 && h >= 1 && d >= 1, family + " has a sub-pixel cuboid");
                        double y = cube.getAsJsonArray("origin").get(1).getAsDouble();
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y + h);
                        var faces = cube.getAsJsonObject("uv");
                        for (String direction : List.of("north", "south", "east", "west", "up", "down")) {
                            var face = faces.getAsJsonObject(direction);
                            double physicalU = direction.equals("east") || direction.equals("west") ? d : w;
                            double physicalV = direction.equals("up") || direction.equals("down") ? d : h;
                            int u = face.getAsJsonArray("uv").get(0).getAsInt();
                            int v = face.getAsJsonArray("uv").get(1).getAsInt();
                            int tw = face.getAsJsonArray("uv_size").get(0).getAsInt();
                            int th = face.getAsJsonArray("uv_size").get(1).getAsInt();
                            assertTrue(
                                    tw >= physicalU && th >= physicalV,
                                    family + " has stretched " + direction + " pixels");
                            assertTrue(u >= 0 && v >= 0 && u + tw <= 128 && v + th <= 128);
                            for (int py = v; py < v + th; py++)
                                for (int px = u; px < u + tw; px++) {
                                    assertEquals(
                                            255,
                                            image.getRGB(px, py) >>> 24,
                                            family + " maps a face into transparency");
                                }
                        }
                    }
                    assertTrue(maxY - minY >= 7, family + " boot is too short");
                    checked++;
                }
                assertEquals(2, checked);
            }
        }
    }

    @Test
    void nativeReptileBootsRenderCompleteQuadsThroughTheVanillaVertexPipeline() {
        var model = new BoneReptileBootsModel<>();
        model.young = false;
        var vertices = new CheckedVertices();
        model.renderToBuffer(new PoseStack(), vertices, 0xF000F0, 0, -1);
        assertEquals(24 * 24, vertices.count);
        assertEquals(vertices.count, vertices.uvCount);
        assertEquals(vertices.count, vertices.normals);
    }

    private static class CheckedVertices implements VertexConsumer {
        int count, uvCount, normals;

        public VertexConsumer addVertex(float x, float y, float z) {
            assertTrue(Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z));
            count++;
            return this;
        }

        public VertexConsumer setColor(int r, int g, int b, int a) {
            return this;
        }

        public VertexConsumer setUv(float u, float v) {
            assertTrue(u >= 0 && v >= 0 && u <= 1 && v <= 1);
            uvCount++;
            return this;
        }

        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        public VertexConsumer setNormal(float x, float y, float z) {
            assertEquals(1, x * x + y * y + z * z, .0001);
            normals++;
            return this;
        }
    }
}
