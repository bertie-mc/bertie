package io.github.bertie_mc.bertieprogression.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.fml.ModList;

/**
 * Builds a vanilla {@link LayerDefinition} out of a Bedrock/Geckolib {@code .geo.json}.
 *
 * <p>Geckolib and AzureLib draw their own geometry, and only for items the mod that owns it
 * registered. Armour registered from outside therefore wears invisibly. A {@code ModelPart} can
 * hold the same shape exactly - unlike an item model element it takes any angle about all three
 * axes - so the geometry is read straight out of the owning mod's jar and rebuilt as an ordinary
 * humanoid layer that {@code HumanoidArmorLayer} renders like any other armour.
 *
 * <p>Armour geometry follows a convention: six root bones named for the humanoid parts, each with
 * the pivot vanilla uses, carrying the actual plates and their decoration beneath. Those six are
 * renamed to what {@link net.minecraft.client.model.HumanoidModel} expects, and everything under
 * them keeps its own name so a slot can show only the parts that belong to it.
 *
 * <p>Two coordinate conventions meet. Bedrock measures up from the feet; a model part measures down
 * from y=24. Cube rotations become child parts, because a part may be rotated and a cube may not.
 */
public final class GeoArmorGeometry {

    private GeoArmorGeometry() {}

    /** Bedrock's root bone names, matched loosely, against the names HumanoidModel demands. */
    private static final Map<String, String> ANCHORS = new LinkedHashMap<>();

    static {
        ANCHORS.put("rightarm", "right_arm");
        ANCHORS.put("leftarm", "left_arm");
        ANCHORS.put("rightleg", "right_leg");
        ANCHORS.put("leftleg", "left_leg");
        ANCHORS.put("head", "head");
        ANCHORS.put("body", "body");
    }

    private static final Map<String, EquipmentSlot> ANCHOR_SLOT = Map.of(
            "head", EquipmentSlot.HEAD,
            "body", EquipmentSlot.CHEST,
            "right_arm", EquipmentSlot.CHEST,
            "left_arm", EquipmentSlot.CHEST,
            "right_leg", EquipmentSlot.LEGS,
            "left_leg", EquipmentSlot.LEGS);

    /** A built layer, plus which of its parts each armour slot should show. */
    public record Built(LayerDefinition layer, Map<EquipmentSlot, List<String[]>> slotParts) {}

    public static Built load(String modId, String geoPath) {
        JsonObject geometry = read(modId, geoPath)
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject desc = geometry.getAsJsonObject("description");
        int texW = desc.has("texture_width") ? desc.get("texture_width").getAsInt() : 16;
        int texH = desc.has("texture_height") ? desc.get("texture_height").getAsInt() : texW;

        List<JsonObject> bones = new ArrayList<>();
        for (JsonElement e : geometry.getAsJsonArray("bones")) {
            bones.add(e.getAsJsonObject());
        }
        Map<String, List<JsonObject>> children = new LinkedHashMap<>();
        Map<String, JsonObject> byName = new LinkedHashMap<>();
        for (JsonObject b : bones) {
            byName.put(name(b), b);
        }
        for (JsonObject b : bones) {
            String parent = b.has("parent") ? b.get("parent").getAsString() : null;
            if (parent != null && byName.containsKey(parent)) {
                children.computeIfAbsent(parent, k -> new ArrayList<>()).add(b);
            }
        }

        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        Map<EquipmentSlot, List<String[]>> slotParts = new EnumMap<>(EquipmentSlot.class);
        boolean hat = false;

        for (JsonObject bone : bones) {
            String parent = bone.has("parent") ? bone.get("parent").getAsString() : null;
            if (parent != null && byName.containsKey(parent)) {
                continue;
            }
            String anchor = anchorFor(name(bone));
            if (anchor == null) {
                continue;
            }
            hat |= anchor.equals("head");
            float[] pivot = pivot(bone);
            PartDefinition part = root.addOrReplaceChild(anchor, CubeListBuilder.create(),
                    PartPose.offset(pivot[0], 24.0F - pivot[1], pivot[2]));
            // Only the anchor's direct children are toggled: hiding a part hides its own children
            // with it, so a slot never has to name the decoration hanging off a plate.
            for (JsonObject child : children.getOrDefault(name(bone), List.of())) {
                EquipmentSlot slot = slotFor(anchor, name(child));
                slotParts.computeIfAbsent(slot, k -> new ArrayList<>())
                        .add(new String[]{anchor, name(child)});
                addBone(part, child, pivot, children);
            }
        }
        if (hat) {
            root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        }
        return new Built(LayerDefinition.create(mesh, texW, texH), slotParts);
    }

    private static void addBone(PartDefinition parent, JsonObject bone, float[] parentPivot,
                                Map<String, List<JsonObject>> children) {
        float[] pivot = pivot(bone);
        float[] rot = rotation(bone);
        CubeListBuilder cubes = CubeListBuilder.create();
        List<JsonObject> rotated = new ArrayList<>();
        for (JsonElement e : bone.has("cubes") ? bone.getAsJsonArray("cubes") : new JsonArray()) {
            JsonObject cube = e.getAsJsonObject();
            if (cube.has("rotation")) {
                rotated.add(cube);
            } else {
                addCube(cubes, cube, pivot);
            }
        }
        PartDefinition part = parent.addOrReplaceChild(name(bone), cubes,
                PartPose.offsetAndRotation(pivot[0] - parentPivot[0],
                        parentPivot[1] - pivot[1], pivot[2] - parentPivot[2],
                        (float) Math.toRadians(-rot[0]), (float) Math.toRadians(rot[1]),
                        (float) Math.toRadians(-rot[2])));
        for (int i = 0; i < rotated.size(); i++) {
            JsonObject cube = rotated.get(i);
            float[] cp = cube.has("pivot") ? floats(cube.getAsJsonArray("pivot")) : pivot;
            float[] cr = floats(cube.getAsJsonArray("rotation"));
            CubeListBuilder one = CubeListBuilder.create();
            addCube(one, cube, cp);
            part.addOrReplaceChild(name(bone) + "_r" + i, one,
                    PartPose.offsetAndRotation(cp[0] - pivot[0], pivot[1] - cp[1], cp[2] - pivot[2],
                            (float) Math.toRadians(-cr[0]), (float) Math.toRadians(cr[1]),
                            (float) Math.toRadians(-cr[2])));
        }
        for (JsonObject child : children.getOrDefault(name(bone), List.of())) {
            addBone(part, child, pivot, children);
        }
    }

    private static void addCube(CubeListBuilder builder, JsonObject cube, float[] pivot) {
        float[] o = floats(cube.getAsJsonArray("origin"));
        float[] s = floats(cube.getAsJsonArray("size"));
        float inflate = cube.has("inflate") ? cube.get("inflate").getAsFloat() : 0.0F;
        float[] uv = cube.has("uv") && cube.get("uv").isJsonArray()
                ? floats(cube.getAsJsonArray("uv")) : null;
        if (uv == null) {
            return;
        }
        builder.texOffs((int) uv[0], (int) uv[1]).addBox(
                o[0] - pivot[0], pivot[1] - o[1] - s[1], o[2] - pivot[2],
                s[0], s[1], s[2], new CubeDeformation(inflate));
    }

    private static String anchorFor(String bone) {
        String lower = bone.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : ANCHORS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static EquipmentSlot slotFor(String anchor, String bone) {
        EquipmentSlot slot = ANCHOR_SLOT.get(anchor);
        return slot == EquipmentSlot.LEGS && bone.toLowerCase(Locale.ROOT).contains("boot")
                ? EquipmentSlot.FEET : slot;
    }

    private static String name(JsonObject bone) {
        return bone.get("name").getAsString();
    }

    private static float[] pivot(JsonObject bone) {
        return bone.has("pivot") ? floats(bone.getAsJsonArray("pivot")) : new float[]{0, 0, 0};
    }

    private static float[] rotation(JsonObject bone) {
        return bone.has("rotation") ? floats(bone.getAsJsonArray("rotation")) : new float[]{0, 0, 0};
    }

    private static float[] floats(JsonArray array) {
        float[] out = new float[array.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }

    private static JsonObject read(String modId, String geoPath) {
        Path path = ModList.get().getModFileById(modId).getFile()
                .findResource(("assets/" + modId + "/" + geoPath).split("/"));
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + geoPath + " from " + modId, e);
        }
    }
}
