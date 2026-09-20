package io.github.bertie_mc.armorcompletions;

import static org.junit.jupiter.api.Assertions.*;
import io.github.bertie_mc.armorcompletions.client.models.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;

class ArmorModelTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void nativeJavaModelsBakeAsHumanoidArmor() {
        var shellLegs = Modelspinyshellarmorleggings.asHumanoidModel(Modelspinyshellarmorleggings.createBodyLayer().bakeRoot());
        var shellBoots = Modelspinyshellarmorboots.asHumanoidModel(Modelspinyshellarmorboots.createBodyLayer().bakeRoot());
        var boneLegs = new BoneReptileLeggingsModel<>(BoneReptileLeggingsModel.createArmorLayer().bakeRoot());
        var boneBoots = new BoneReptileBootsModel<>(BoneReptileBootsModel.createArmorLayer().bakeRoot());
        for (var model : java.util.List.of(shellLegs, shellBoots, boneLegs, boneBoots)) {
            assertNotSame(model.leftLeg, model.rightLeg);
            assertEquals(12, model.leftLeg.y);
            assertEquals(12, model.rightLeg.y);
            assertTrue(model.leftLeg.getAllParts().count() > 1);
            assertTrue(model.rightLeg.getAllParts().count() > 1);
        }
    }

    @Test void geckoLibBakesEachCompletedGeometryWithWearableBootBones() throws Exception {
        for (String family : java.util.List.of("bishop_of_deceit", "pyromancer_brute", "nameless_one", "necromancer")) {
            String file = "/assets/hazennstuff/geo/armor/" + family + "_completed.geo.json";
            try (var stream = getClass().getResourceAsStream(file)) {
                assertNotNull(stream, file);
                Model raw = KeyFramesAdapter.GEO_GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Model.class);
                var baked = new BakedModelFactory.Builtin().constructGeoModel(GeometryTree.fromModel(raw));
                for (String side : java.util.List.of("Left", "Right")) {
                    assertFalse(baked.getBone("armor" + side + "Boot").orElseThrow().getCubes().isEmpty());
                    assertFalse(baked.getBone("armor" + side + "Leg").orElseThrow().getCubes().isEmpty());
                }
            }
            try (var stream = getClass().getResourceAsStream("/assets/hazennstuff/textures/armor/" + family + "_completed.png")) {
                var image = ImageIO.read(stream);
                assertEquals(128, image.getWidth());
                assertEquals(128, image.getHeight());
            }
        }
    }

    @Test void everyNewInventoryModelResolvesToItsApprovedSprite() throws Exception {
        int count = 0;
        for (ArmorFamily family : ArmorFamily.values()) {
            for (var type : family.missingTypes()) {
                String path = "/assets/armorcompletions/models/item/" + family.itemName(type) + ".json";
                try (var stream = getClass().getResourceAsStream(path)) {
                    assertNotNull(stream, path);
                    var json = com.google.gson.JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                    String[] texture = json.getAsJsonObject("textures").get("layer0").getAsString().split(":");
                    try (var png = getClass().getResourceAsStream("/assets/" + texture[0] + "/textures/" + texture[1] + ".png")) {
                        assertNotNull(png, path);
                        var image = ImageIO.read(png);
                        assertEquals(16, image.getWidth());
                        assertEquals(16, image.getHeight());
                    }
                }
                count++;
            }
        }
        assertEquals(10, count);
    }
}
