package io.github.bertie_mc.bertieprogression.ponder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
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
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(SpatialPonderPlugin::screenOpening);
    }

    private static void screenOpening(net.neoforged.neoforge.client.event.ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof PonderUI ui) || ui instanceof RecipeScreen) return;
        if (guideForScene(ui.getActiveScene()) == null) return;
        var subject = BuiltInRegistries.ITEM.getKey(ui.getSubject().getItem());
        var replacement = new RecipeScreen(PonderIndex.getSceneAccess().compile(subject));
        replacement.shareContextWith(ui);
        event.setNewScreen(replacement);
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
        return new RecipeScreen(recipe);
    }

    private static JsonObject guide(ResourceLocation recipe) {
        return GUIDES.stream()
                .filter(row -> row.get("recipe").getAsString().equals(recipe.toString()))
                .findFirst()
                .orElseThrow();
    }

    private static JsonObject guideForScene(net.createmod.ponder.foundation.PonderScene scene) {
        return GUIDES.stream()
                .filter(row ->
                        row.get("scene").getAsString().equals(scene.getId().toString()))
                .findFirst()
                .orElse(null);
    }

    private static ItemStack material(String id) {
        if (id.equals("mekanism:bounding_block")) return ItemStack.EMPTY;
        if (id.equals("oritech:superconductor_connection"))
            return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("oritech:superconductor")));
        return new ItemStack(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id)));
    }

    private static List<ItemStack> stepItems(JsonObject guide) {
        var result = new ArrayList<ItemStack>();
        if (guide.get("kind").getAsString().equals("mini"))
            result.add(new ItemStack(
                    BuiltInRegistries.ITEM.get(ResourceLocation.parse("compactcrafting:field_projector"))));
        var seen = new LinkedHashSet<net.minecraft.world.item.Item>();
        for (int y = 0; y < guide.get("size").getAsInt(); y++) {
            for (var element : guide.getAsJsonArray("blocks")) {
                var cell = element.getAsJsonObject();
                if (cell.getAsJsonArray("pos").get(1).getAsInt() != y) continue;
                var item = material(cell.getAsJsonObject("state").get("Name").getAsString());
                if (!item.isEmpty() && seen.add(item.getItem())) result.add(item);
            }
        }
        result.add(new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.parse(guide.get("catalyst").getAsString()))));
        var output = new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.parse(guide.getAsJsonObject("output").get("id").getAsString())));
        if (guide.has("block_output")) {
            result.add(output.copy());
            result.add(new ItemStack(
                    BuiltInRegistries.ITEM.get(ResourceLocation.parse("anvilcraft:space_overcompressor"))));
        }
        result.add(output);
        return List.copyOf(result);
    }

    public static class RecipeScreen extends PonderUI {
        private List<ItemStack> steps = List.of();
        private ResourceLocation activeGuide;
        private final List<StepButton> stepButtons = new ArrayList<>();

        public RecipeScreen(ResourceLocation recipe) {
            this(scenes(recipe));
        }

        private RecipeScreen(List<net.createmod.ponder.foundation.PonderScene> scenes) {
            super(scenes);
            updateActiveSteps();
        }

        private boolean updateActiveSteps() {
            var scene = getActiveScene();
            if (scene.getId().equals(activeGuide)) return false;
            activeGuide = scene.getId();
            var guide = guideForScene(scene);
            steps = guide == null ? List.of() : stepItems(guide);
            return true;
        }

        public int stepCount() {
            return steps.size();
        }

        public void seekStep(int index) {
            var scene = getActiveScene();
            int offset = scene.getKeyframeCount() - steps.size();
            if (offset < 0) throw new IllegalStateException("Missing spatial guide keyframes");
            seekToTime(scene.getKeyframeTime(index + offset) + 1);
        }

        @Override
        protected void init() {
            super.init();
            updateActiveSteps();
            rebuildButtons();
        }

        private void rebuildButtons() {
            for (var button : stepButtons) removeWidget(button);
            stepButtons.clear();
            int columns = Math.max(1, Math.min(steps.size(), (width - 24) / 22));
            int rows = (steps.size() + columns - 1) / columns;
            for (int i = 0; i < steps.size(); i++) {
                final int index = i;
                int row = i / columns;
                int cells = Math.min(columns, steps.size() - row * columns);
                int x = (width - cells * 22) / 2 + i % columns * 22;
                int y = rows == 1 ? height - 22 : height - 55 - (rows - row) * 22;
                var button = new StepButton(x, y, steps.get(i), () -> seekStep(index));
                stepButtons.add(button);
                addWidget(button);
            }
        }

        public ItemStack stepItemAt(double mouseX, double mouseY) {
            for (var button : stepButtons) if (button.isMouseOver(mouseX, mouseY)) return button.item;
            return ItemStack.EMPTY;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            for (var step : stepButtons) if (step.mouseClicked(mouseX, mouseY, button)) return true;
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        protected void renderWidgets(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (updateActiveSteps()) rebuildButtons();
            super.renderWidgets(graphics, mouseX, mouseY, partialTicks);
            // Item rendering enables depth testing; keep the whole strip above the scene.
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 500);
            for (var button : stepButtons) button.render(graphics, mouseX, mouseY, partialTicks);
            var item = stepItemAt(mouseX, mouseY);
            if (!item.isEmpty()) graphics.renderTooltip(font, item.getHoverName(), mouseX, mouseY);
            graphics.pose().popPose();
        }
    }

    private static final class StepButton extends AbstractButton {
        private final ItemStack item;
        private final Runnable action;

        private StepButton(int x, int y, ItemStack item, Runnable action) {
            super(x, y, 20, 20, item.getHoverName());
            this.item = item;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            graphics.blitSprite(
                    ResourceLocation.withDefaultNamespace(
                            isHoveredOrFocused() ? "widget/button_highlighted" : "widget/button"),
                    getX(),
                    getY(),
                    width,
                    height);
            graphics.renderItem(item, getX() + 2, getY() + 2);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
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
        scene.addInstruction(current -> current.getTransform().yRotation.startWithValue(-30));
        scene.showBasePlate();
        if (!giant) {
            scene.idle(1);
            scene.addKeyframe();
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
                var icon = material(id);
                if (icon.isEmpty()
                        || !labelled.add(
                                BuiltInRegistries.ITEM.getKey(icon.getItem()).toString())) continue;
                scene.addKeyframe();
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
        scene.addKeyframe();
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
            scene.addKeyframe();
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
        scene.addKeyframe();
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
