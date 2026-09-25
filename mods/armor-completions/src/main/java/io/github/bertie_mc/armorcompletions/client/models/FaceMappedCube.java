package io.github.bertie_mc.armorcompletions.client.models;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Native cuboid whose face UVs are independent of its physical dimensions. */
final class FaceMappedCube extends ModelPart.Cube {
    private record Face(float[][] vertices, float[] uv, Vector3f normal) {}

    private final List<Face> faces = new ArrayList<>();

    FaceMappedCube(float x, float y, float z, float w, float h, float d, JsonObject mapping) {
        super(0, 0, x, y, z, w, h, d, 0, 0, 0, false, 128, 128, Set.of());
        float x1 = x + w;
        float y1 = y + h;
        float z1 = z + d;
        face(mapping, "north", new float[][] {{x1, y, z}, {x, y, z}, {x, y1, z}, {x1, y1, z}}, 0, 0, -1);
        face(mapping, "south", new float[][] {{x, y, z1}, {x1, y, z1}, {x1, y1, z1}, {x, y1, z1}}, 0, 0, 1);
        face(mapping, "east", new float[][] {{x, y, z}, {x, y, z1}, {x, y1, z1}, {x, y1, z}}, -1, 0, 0);
        face(mapping, "west", new float[][] {{x1, y, z1}, {x1, y, z}, {x1, y1, z}, {x1, y1, z1}}, 1, 0, 0);
        face(mapping, "up", new float[][] {{x1, y, z1}, {x, y, z1}, {x, y, z}, {x1, y, z}}, 0, -1, 0);
        face(mapping, "down", new float[][] {{x1, y1, z}, {x, y1, z}, {x, y1, z1}, {x1, y1, z1}}, 0, 1, 0);
    }

    private void face(JsonObject mapping, String name, float[][] vertices, float nx, float ny, float nz) {
        var entry = mapping.getAsJsonObject(name);
        var origin = entry.getAsJsonArray("uv");
        var size = entry.getAsJsonArray("uv_size");
        float u = origin.get(0).getAsFloat() / 128;
        float v = origin.get(1).getAsFloat() / 128;
        float u1 = u + size.get(0).getAsFloat() / 128;
        float v1 = v + size.get(1).getAsFloat() / 128;
        faces.add(new Face(vertices, new float[] {u1, v, u, v, u, v1, u1, v1}, new Vector3f(nx, ny, nz)));
    }

    @Override
    public void compile(PoseStack.Pose pose, VertexConsumer output, int light, int overlay, int color) {
        Vector3f point = new Vector3f();
        Vector3f normal = new Vector3f();
        for (Face face : faces) {
            pose.transformNormal(face.normal(), normal);
            for (int i = 0; i < 4; i++) {
                float[] vertex = face.vertices()[i];
                pose.pose().transformPosition(vertex[0] / 16, vertex[1] / 16, vertex[2] / 16, point);
                output.addVertex(
                        point.x(),
                        point.y(),
                        point.z(),
                        color,
                        face.uv()[i * 2],
                        face.uv()[i * 2 + 1],
                        overlay,
                        light,
                        normal.x(),
                        normal.y(),
                        normal.z());
            }
        }
    }
}
