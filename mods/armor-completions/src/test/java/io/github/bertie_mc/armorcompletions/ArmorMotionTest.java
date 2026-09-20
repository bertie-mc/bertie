package io.github.bertie_mc.armorcompletions;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.model.GeoModel;

class ArmorMotionTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test void bothWizardSetsBindTheVisibleLegAndBootToTheCorrectWalkingLeg() throws Exception {
        for (String family : List.of("nameless_one", "necromancer")) {
            JsonObject json = geometry(family);
            for (String side : List.of("Left", "Right")) {
                for (String part : List.of("Leg", "Boot")) {
                    var bone = bone(json, "armor" + side + part);
                    assertEquals("biped" + side + "Leg", bone.get("parent").getAsString());
                    for (var element : bone.getAsJsonArray("cubes")) {
                        var cube = element.getAsJsonObject();
                        double center = cube.getAsJsonArray("origin").get(0).getAsDouble() + cube.getAsJsonArray("size").get(0).getAsDouble() / 2;
                        assertTrue(side.equals("Left") ? center > 0 : center < 0,
                                family + " " + side + part + " is attached to the opposite side");
                    }
                }
            }
            withRenderer(json, renderer -> {
                var pose = humanoid();
                pose.leftLeg.xRot = .73f;
                pose.rightLeg.xRot = -.29f;
                renderer.pose(pose, EquipmentSlot.LEGS);
                assertRotation(renderer.baked, "armorLeftLeg", .73f);
                assertRotation(renderer.baked, "armorRightLeg", .29f);
                renderer.pose(pose, EquipmentSlot.FEET);
                assertRotation(renderer.baked, "armorLeftBoot", .73f);
                assertRotation(renderer.baked, "armorRightBoot", .29f);
                pose.leftLeg.xRot = -.19f;
                pose.rightLeg.xRot = .61f;
                renderer.pose(pose, EquipmentSlot.FEET);
                assertRotation(renderer.baked, "armorLeftBoot", .19f);
                assertRotation(renderer.baked, "armorRightBoot", .61f);
            });
        }
    }

    @Test void bishopRobePanelsFollowIndependentLegPosesAndRemainChestSlotGeometry() throws Exception {
        JsonObject json = geometry("bishop_of_deceit");
        assertFalse(bone(json, "skirt").has("cubes"));
        for (String side : List.of("Left", "Right")) {
            var panel = bone(json, "armorTorsoExtension" + side + "Leg");
            assertEquals("biped" + side + "Leg", panel.get("parent").getAsString());
            assertEquals(1, panel.getAsJsonArray("cubes").size());
        }
        withRenderer(json, renderer -> {
            var pose = humanoid();
            pose.leftLeg.xRot = .65f;
            pose.rightLeg.xRot = -.4f;
            renderer.pose(pose, EquipmentSlot.CHEST);
            assertRotation(renderer.baked, "armorTorsoExtensionLeftLeg", .65f);
            assertRotation(renderer.baked, "armorTorsoExtensionRightLeg", .4f);
            assertFalse(renderer.baked.getBone("armorTorsoExtensionLeftLeg").orElseThrow().isHidden());
            assertFalse(renderer.baked.getBone("armorTorsoExtensionRightLeg").orElseThrow().isHidden());
            renderer.pose(pose, EquipmentSlot.LEGS);
            assertTrue(renderer.baked.getBone("armorTorsoExtensionLeftLeg").orElseThrow().isHidden());
            assertTrue(renderer.baked.getBone("armorTorsoExtensionRightLeg").orElseThrow().isHidden());
        });
    }

    @Test void originalItemsHaveValidClientMixinTargetsAndCompletedResources() throws Exception {
        for (String[] pair : List.of(
                new String[]{"BishopOfDeceitArmor", "BishopOfDeceitArmor", "bishop_of_deceit"},
                new String[]{"NamelessOneArmor", "NamelessOneArmor", "nameless_one"},
                new String[]{"NecromancerArmor", "NecromancerArmor", "necromancer"})) {
            String prefix = "net.hazen.hazennstuff.Item.Armor.Misc." + pair[0] + "." + pair[1];
            var model = Class.forName(prefix + "Model");
            var item = Class.forName(prefix + "Item");
            assertEquals(ResourceLocation.class, model.getDeclaredMethod("getModelResource", item).getReturnType());
            assertNotNull(geometry(pair[2]));
        }
        try (var stream = getClass().getResourceAsStream("/armorcompletions.mixins.json")) {
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(3, json.getAsJsonArray("client").size());
            assertTrue(json.get("required").getAsBoolean());
        }
    }

    private static void assertRotation(BakedGeoModel model, String name, float angle) {
        assertEquals(angle, Math.abs(model.getBone(name).orElseThrow().getRotX()), 0.0001f, name);
    }
    private static HumanoidModel<?> humanoid() {
        return new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0), 64, 32).bakeRoot());
    }
    private static JsonObject geometry(String family) throws Exception {
        try (var stream = ArmorMotionTest.class.getResourceAsStream("/assets/hazennstuff/geo/armor/" + family + "_completed.geo.json")) {
            assertNotNull(stream);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    private static JsonObject bone(JsonObject json, String name) {
        for (var element : json.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
            var b = element.getAsJsonObject();if (name.equals(b.get("name").getAsString())) return b;
        }
        throw new AssertionError("Missing bone " + name);
    }

    // The real armor renderer needs only Minecraft's model set during construction.
    // Supply that one field without opening a window, audio device or game session.
    private static void withRenderer(JsonObject json, java.util.function.Consumer<RendererHarness> check) throws Exception {
        Field singleton = Minecraft.class.getDeclaredField("instance");singleton.setAccessible(true);
        Object previous = singleton.get(null);
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
        var unsafe = (sun.misc.Unsafe)unsafeField.get(null);
        var client = (Minecraft)unsafe.allocateInstance(Minecraft.class);
        var models = new EntityModelSet();
        Field roots = EntityModelSet.class.getDeclaredField("roots");roots.setAccessible(true);
        roots.set(models, Map.of(ModelLayers.PLAYER_INNER_ARMOR, LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0), 64, 32)));
        Field modelSet = Minecraft.class.getDeclaredField("entityModels");modelSet.setAccessible(true);modelSet.set(client, models);
        try {
            singleton.set(null, client);
            Model raw = KeyFramesAdapter.GEO_GSON.fromJson(json, Model.class);
            BakedGeoModel baked = new BakedModelFactory.Builtin().constructGeoModel(GeometryTree.fromModel(raw));
            var renderer = new RendererHarness(new TestModel(baked), baked);
            check.accept(renderer);
        } finally { singleton.set(null, previous); }
    }
    private static final class TestItem extends Item implements GeoItem {
        TestItem() { super(new Properties()); }
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
        public AnimatableInstanceCache getAnimatableInstanceCache() { return null; }
    }
    private static final class TestModel extends GeoModel<TestItem> {
        final BakedGeoModel baked;
        TestModel(BakedGeoModel baked) { this.baked = baked; }
        @Override public Optional<GeoBone> getBone(String name) { return baked.getBone(name); }
        @Override public ResourceLocation getModelResource(TestItem item) { return ResourceLocation.fromNamespaceAndPath("armorcompletions", "motion_test"); }
        @Override public ResourceLocation getTextureResource(TestItem item) { return getModelResource(item); }
        @Override public ResourceLocation getAnimationResource(TestItem item) { return getModelResource(item); }
    }
    private static final class RendererHarness extends GenericCustomArmorRenderer<TestItem> {
        final BakedGeoModel baked;
        RendererHarness(TestModel model, BakedGeoModel baked) { super(model); this.baked = baked; grabRelevantBones(baked); }
        void pose(HumanoidModel<?> pose, EquipmentSlot slot) { currentSlot = slot; applyBaseTransformations(pose); applyBoneVisibilityBySlot(slot); }
    }
}
