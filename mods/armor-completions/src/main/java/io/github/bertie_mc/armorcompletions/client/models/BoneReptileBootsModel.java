package io.github.bertie_mc.armorcompletions.client.models;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

/** Loads native cuboids with enough atlas detail for each physical face. */
public final class BoneReptileBootsModel<T extends LivingEntity> extends HumanoidModel<T> {
    public BoneReptileBootsModel() {
        super(createRoot());
    }

    private static ModelPart createRoot() {
        Map<String, ModelPart> roots = new LinkedHashMap<>();
        for (String name : List.of("head", "hat", "body", "right_arm", "left_arm")) {
            roots.put(name, new ModelPart(List.of(), Map.of()));
        }
        String resource = "/assets/armorcompletions/geometry/bone_reptile_boots.json";
        try (var input = BoneReptileBootsModel.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Missing boot geometry: " + resource);
            var data = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (var element : data.getAsJsonArray("bones")) {
                var bone = element.getAsJsonObject();
                var pivot = bone.getAsJsonArray("pivot");
                float px = -pivot.get(0).getAsFloat();
                float py = 24 - pivot.get(1).getAsFloat();
                float pz = pivot.get(2).getAsFloat();
                Map<String, ModelPart> details = new LinkedHashMap<>();
                for (var box : bone.getAsJsonArray("cubes")) {
                    var cube = box.getAsJsonObject();
                    var origin = cube.getAsJsonArray("origin");
                    var size = cube.getAsJsonArray("size");
                    float w = size.get(0).getAsFloat();
                    float h = size.get(1).getAsFloat();
                    float d = size.get(2).getAsFloat();
                    var mapped = new FaceMappedCube(
                            -origin.get(0).getAsFloat() - w - px,
                            24 - origin.get(1).getAsFloat() - h - py,
                            origin.get(2).getAsFloat() - pz,
                            w,
                            h,
                            d,
                            cube.getAsJsonObject("uv"));
                    details.put(cube.get("name").getAsString(), new ModelPart(List.of(mapped), Map.of()));
                }
                var part = new ModelPart(List.of(), details);
                part.setPos(px, py, pz);
                roots.put(bone.get("name").getAsString().equals("armorLeftBoot") ? "right_leg" : "left_leg", part);
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not load boot geometry", exception);
        }
        return new ModelPart(List.of(), roots);
    }
}
