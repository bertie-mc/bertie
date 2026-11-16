package io.github.bertie_mc.bertieprogression.ponder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** One visual assembly guide per owned spatial recipe. */
public final class SpatialPonderPlugin implements PonderPlugin {
    private static final List<JsonObject> GUIDES = load();

    private static List<JsonObject> load() {
        try (var input = SpatialPonderPlugin.class.getResourceAsStream("/spatial_guides.json")) {
            if (input == null) return List.of();
            var result = new ArrayList<JsonObject>();
            for (var row : JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonArray()) {
                result.add(row.getAsJsonObject());
            }
            return List.copyOf(result);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Unable to load spatial guides", e);
        }
    }

    public static void register() {
        PonderIndex.addPlugin(new SpatialPonderPlugin());
    }

    public static void open(ResourceLocation recipe) {
        Minecraft.getInstance().setScreen(screen(recipe));
    }

    public static List<net.createmod.ponder.foundation.PonderScene> scenes(ResourceLocation recipe) {
        var guide = GUIDES.stream()
                .filter(row -> row.get("recipe").getAsString().equals(recipe.toString()))
                .findFirst()
                .orElseThrow();
        var subject =
                ResourceLocation.parse(guide.getAsJsonObject("output").get("id").getAsString());
        var id = ResourceLocation.parse(guide.get("scene").getAsString());
        return PonderIndex.getSceneAccess().compile(subject).stream()
                .filter(scene -> id.equals(scene.getId()))
                .toList();
    }

    public static PonderUI screen(ResourceLocation recipe) {
        return new RecipeScreen(scenes(recipe));
    }

    private static final class RecipeScreen extends PonderUI {
        private RecipeScreen(List<net.createmod.ponder.foundation.PonderScene> scenes) {
            super(scenes);
        }
    }

    @Override
    public String getModId() {
        return "bertieprogression";
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        for (var guide : GUIDES) {
            var subject = ResourceLocation.parse(
                    guide.getAsJsonObject("output").get("id").getAsString());
            if (!BuiltInRegistries.ITEM.containsKey(subject)) continue;
            var scene = ResourceLocation.parse(guide.get("scene").getAsString());
            helper.addStoryBoard(subject, scene, (builder, util) -> assemble(builder, util, guide));
        }
    }

    private static void assemble(SceneBuilder scene, SceneBuildingUtil util, JsonObject guide) {
        int size = guide.get("size").getAsInt();
        boolean giant = guide.get("kind").getAsString().equals("giant");
        var outputId =
                ResourceLocation.parse(guide.getAsJsonObject("output").get("id").getAsString());
        scene.title(
                ResourceLocation.parse(guide.get("scene").getAsString()).getPath(),
                BuiltInRegistries.ITEM.get(outputId).getDescription().getString());
        scene.configureBasePlate(0, 0, size + 2);
        scene.scaleSceneView(Math.min(0.9f, 5.0f / (size + 2)));
        scene.setSceneOffsetY(-1);
        scene.addInstruction(current -> current.getTransform().yRotation.startWithValue(0));
        scene.showBasePlate();
        if (!giant) {
            for (int x : new int[] {0, size + 1})
                for (int z : new int[] {0, size + 1}) {
                    scene.world().showSection(util.select().position(x, 1, z), Direction.DOWN);
                }
            scene.overlay()
                    .showOutline(PonderPalette.BLUE, "field", util.select().fromTo(1, 1, 1, size, size, size), 1200);
        }
        var labelled = new HashSet<String>();
        for (int y = 0; y < size; y++) {
            scene.world().showSection(util.select().fromTo(1, y + 1, 1, size, y + 1, size), Direction.DOWN);
            scene.idle(12);
            for (var element : guide.getAsJsonArray("blocks")) {
                var cell = element.getAsJsonObject();
                var pos = cell.getAsJsonArray("pos");
                if (pos.get(1).getAsInt() != y) continue;
                String id = cell.getAsJsonObject("state").get("Name").getAsString();
                if (id.equals("mekanism:bounding_block") || !labelled.add(id)) continue;
                var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
                String name = id.equals("oritech:superconductor_connection")
                        ? BuiltInRegistries.ITEM
                                .get(ResourceLocation.parse("oritech:superconductor"))
                                .getDescription()
                                .getString()
                        : block.getName().getString();
                scene.overlay()
                        .showText(35)
                        .text(name)
                        .placeNearTarget()
                        .pointAt(util.vector()
                                .topOf(
                                        pos.get(0).getAsInt() + 1,
                                        y + 1,
                                        pos.get(2).getAsInt() + 1));
                scene.idle(40);
            }
        }
        double centre = (size + 2) / 2.0;
        String catalyst = guide.get("catalyst").getAsString();
        var catalystItem = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(catalyst)));
        if (giant) {
            scene.world().showSection(util.select().layer(size + 1), Direction.DOWN);
            scene.idle(15);
        }
        scene.overlay()
                .showText(40)
                .text(catalystItem.getHoverName().getString())
                .placeNearTarget()
                .pointAt(new Vec3(centre, size + 2, centre));
        if (giant) giantFall(scene, centre, size, catalystItem);
        else {
            var dropped = scene.world()
                    .createItemEntity(new Vec3(centre, size + 4, centre), new Vec3(0, -0.15, 0), catalystItem);
            scene.idle(35);
            scene.world().modifyEntity(dropped, entity -> entity.discard());
        }
        scene.world().hideSection(util.select().fromTo(1, 1, 1, size, giant ? size + 1 : size, size), Direction.UP);
        scene.idle(10);
        var ops = RegistryOps.create(
                JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        ItemStack output = ItemStack.CODEC.parse(ops, guide.get("output")).getOrThrow();
        if (guide.has("block_output")) {
            var core = new BlockPos(size / 2 + 1, size / 2 + 1, size / 2 + 1);
            scene.world()
                    .setBlock(
                            core,
                            BuiltInRegistries.BLOCK
                                    .get(ResourceLocation.parse(
                                            guide.get("block_output").getAsString()))
                                    .defaultBlockState(),
                            false);
            scene.world().showSection(util.select().position(core), Direction.DOWN);
            scene.overlay()
                    .showText(40)
                    .text(output.getHoverName().getString())
                    .placeNearTarget()
                    .pointAt(util.vector().topOf(core));
            scene.idle(45);
            scene.addKeyframe();
            for (var element : guide.getAsJsonArray("blocks")) {
                var cell = element.getAsJsonObject();
                var pos = cell.getAsJsonArray("pos");
                if (pos.get(0).getAsInt() == size / 2
                        && pos.get(1).getAsInt() == size / 2
                        && pos.get(2).getAsInt() == size / 2)
                    scene.world()
                            .setBlock(
                                    core,
                                    BuiltInRegistries.BLOCK
                                            .get(ResourceLocation.parse(cell.getAsJsonObject("state")
                                                    .get("Name")
                                                    .getAsString()))
                                            .defaultBlockState(),
                                    false);
            }
            scene.world().showSection(util.select().fromTo(1, 1, 1, size, size + 1, size), Direction.DOWN);
            var cap = new BlockPos(size / 2 + 1, size + 1, size / 2 + 1);
            var compressor = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("anvilcraft:space_overcompressor"));
            scene.world().setBlock(cap, compressor.defaultBlockState(), false);
            scene.overlay()
                    .showText(40)
                    .text(compressor.getName().getString())
                    .placeNearTarget()
                    .pointAt(util.vector().topOf(cap));
            scene.idle(45);
            giantFall(scene, centre, size, catalystItem);
            scene.world().hideSection(util.select().fromTo(1, 1, 1, size, size + 1, size), Direction.UP);
            scene.idle(10);
        }
        scene.world().createItemEntity(new Vec3(centre, 1.5, centre), Vec3.ZERO, output);
        scene.overlay()
                .showText(45)
                .text(output.getHoverName().getString())
                .placeNearTarget()
                .pointAt(new Vec3(centre, 1.5, centre));
        scene.idle(55);
        scene.markAsFinished();
    }

    private static void giantFall(SceneBuilder scene, double centre, int size, ItemStack catalystItem) {
        var dropped = scene.world().createEntity(level -> {
            var entity = BuiltInRegistries.ENTITY_TYPE
                    .getOptional(ResourceLocation.parse("anvilcraft:falling_giant_anvil"))
                    .map(type -> type.create(level))
                    .orElse(null);
            if (entity == null)
                entity = new net.minecraft.world.entity.item.ItemEntity(level, centre, size + 5, centre, catalystItem);
            if (entity instanceof net.minecraft.world.entity.item.FallingBlockEntity) {
                var data = new net.minecraft.nbt.CompoundTag();
                var state = new net.minecraft.nbt.CompoundTag();
                state.putString("Name", "anvilcraft:giant_anvil");
                data.put("BlockState", state);
                data.putBoolean("DropItem", false);
                entity.load(data);
            }
            entity.setPos(centre, size + 5, centre);
            entity.setNoGravity(true);
            return entity;
        });
        for (int frame = 0; frame < 13; frame++) {
            double height = size + 5 - frame * 0.25;
            scene.world().modifyEntity(dropped, entity -> entity.setPos(centre, height, centre));
            scene.idle(2);
        }
        scene.world().modifyEntity(dropped, entity -> entity.discard());
    }
}
