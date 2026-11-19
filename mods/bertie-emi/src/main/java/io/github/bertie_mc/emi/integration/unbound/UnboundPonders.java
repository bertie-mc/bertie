package io.github.bertie_mc.emi.integration.unbound;

import java.util.*;
import java.util.function.Supplier;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Assembly positions come from Unbound's own validators. */
public final class UnboundPonders implements PonderPlugin {
    public record Guide(
            String id,
            ResourceLocation subject,
            Map<BlockPos, BlockState> cells,
            List<ItemStack> materials,
            int span,
            int height) {}

    private static List<Guide> guides = List.of();

    public static List<Guide> guides() {
        return guides;
    }

    public static void register() {
        try {
            var result = new ArrayList<Guide>();
            Class<?> tiers = Class.forName("me.moltea.unbound.rocket.RocketTier");
            Class<?> structures = Class.forName("me.moltea.unbound.rocket.RocketStructure");
            Class<?> blocks = Class.forName("me.moltea.unbound.registry.ModBlocks");
            for (Object tier : tiers.getEnumConstants()) {
                Block cabin =
                        (Block) ((Supplier<?>) blocks.getMethod("rocket", tiers).invoke(null, tier)).get();
                Object structure = structures.getMethod("of", tiers).invoke(null, tier);
                Map<BlockPos, BlockState> cells = new LinkedHashMap<>();
                cells.put(BlockPos.ZERO, cabin.defaultBlockState());
                for (Object piece : (List<?>) structures.getMethod("pieces").invoke(structure)) {
                    BlockPos pos =
                            (BlockPos) piece.getClass().getMethod("offset").invoke(piece);
                    Block part = (Block)
                            ((Supplier<?>) piece.getClass().getMethod("block").invoke(piece)).get();
                    cells.put(pos, part.defaultBlockState());
                }
                result.add(guide(
                        ((Enum<?>) tier).name().toLowerCase(Locale.ROOT),
                        BuiltInRegistries.BLOCK.getKey(cabin),
                        cells));
            }
            var heart = block("orbital_heart");
            var cells = new LinkedHashMap<BlockPos, BlockState>();
            cells.put(BlockPos.ZERO, heart.defaultBlockState());
            Class<?> emitter = Class.forName("me.moltea.unbound.space.emitter.EmitterStructure");
            for (Object piece :
                    (List<?>) emitter.getMethod("pieces", BlockPos.class).invoke(null, BlockPos.ZERO)) {
                var pos = (BlockPos) piece.getClass().getMethod("pos").invoke(piece);
                boolean pylon = (boolean) piece.getClass().getMethod("pylon").invoke(piece);
                cells.put(pos, block(pylon ? "orbital_pylon" : "orbital_plate").defaultBlockState());
            }
            result.add(guide("emitter", BuiltInRegistries.BLOCK.getKey(heart), cells));
            cells = new LinkedHashMap<>();
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++)
                    cells.put(new BlockPos(x, 0, z), block("launch_pad").defaultBlockState());
            result.add(guide("launch_pad", ResourceLocation.parse("unbound:launch_pad"), cells));
            guides = List.copyOf(result);
            PonderIndex.addPlugin(new UnboundPonders());
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(UnboundPonders::opening);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Unsupported Unbound assembly API", failure);
        }
    }

    private static Block block(String name) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("unbound:" + name));
    }

    private static Guide guide(String id, ResourceLocation subject, Map<BlockPos, BlockState> original) {
        int minX = original.keySet().stream().mapToInt(BlockPos::getX).min().orElse(0),
                minY = original.keySet().stream().mapToInt(BlockPos::getY).min().orElse(0),
                minZ = original.keySet().stream().mapToInt(BlockPos::getZ).min().orElse(0);
        var cells = new LinkedHashMap<BlockPos, BlockState>();
        var counts = new LinkedHashMap<Block, Integer>();
        original.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getKey().getY()))
                .forEach(e -> {
                    cells.put(e.getKey().offset(1 - minX, 1 - minY, 1 - minZ), e.getValue());
                    counts.merge(e.getValue().getBlock(), 1, Integer::sum);
                });
        var items = new ArrayList<ItemStack>();
        counts.forEach((block, count) -> items.add(new ItemStack(block, count)));
        int span = cells.keySet().stream()
                .mapToInt(p -> Math.max(p.getX(), p.getZ()) + 2)
                .max()
                .orElse(3);
        int height = cells.keySet().stream().mapToInt(BlockPos::getY).max().orElse(1);
        return new Guide(id, subject, Collections.unmodifiableMap(cells), List.copyOf(items), span, height);
    }

    @Override
    public String getModId() {
        return "bertieemi";
    }

    @Override
    public void onPonderLevelRestore(net.createmod.ponder.api.level.PonderLevel level) {
        if (level.scene == null) return;
        var guide = forScene(level.scene);
        if (guide == null) return;
        // Blank templates have only a base layer; expand their bounds before placing native parts.
        level.getBounds().encapsulate(new BlockPos(guide.span(), guide.height() + 1, guide.span()));
        guide.cells().forEach((pos, state) -> level.setBlock(pos, state, 2));
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        for (var guide : guides) {
            var subjects = new LinkedHashSet<ResourceLocation>();
            subjects.add(guide.subject());
            for (var stack : guide.materials()) subjects.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            if (guide.id().equals("emitter")) subjects.add(ResourceLocation.parse("unbound:station_seed"));
            for (var subject : subjects)
                helper.addStoryBoard(subject, sceneId(guide), (scene, util) -> assemble(scene, util, guide));
        }
    }

    private static ResourceLocation sceneId(Guide guide) {
        return ResourceLocation.parse("bertieemi:unbound/" + guide.id());
    }

    private static void assemble(SceneBuilder scene, SceneBuildingUtil util, Guide guide) {
        scene.title(
                "unbound/" + guide.id(),
                BuiltInRegistries.ITEM.get(guide.subject()).getDescription().getString());
        scene.configureBasePlate(0, 0, guide.span());
        scene.scaleSceneView(Math.min(.75f, 3.6f / Math.max(guide.span(), guide.height())));
        scene.setSceneOffsetY(-1);
        scene.addInstruction(current -> current.getTransform().yRotation.startWithValue(-30));
        scene.showBasePlate();
        scene.idle(5);
        for (var item : guide.materials()) {
            scene.addKeyframe();
            BlockPos label = null;
            for (var cell : guide.cells().entrySet())
                if (cell.getValue().getBlock().asItem() == item.getItem()) {
                    scene.world().showSection(util.select().position(cell.getKey()), Direction.DOWN);
                    label = cell.getKey();
                }
            if (label != null)
                scene.overlay()
                        .showText(35)
                        .text(item.getHoverName().getString())
                        .placeNearTarget()
                        .pointAt(util.vector().topOf(label));
            scene.idle(40);
        }
        scene.markAsFinished();
    }

    public static void open(ResourceLocation subject) {
        Minecraft.getInstance()
                .setScreen(new AssemblyScreen(PonderIndex.getSceneAccess().compile(subject)));
    }

    public static void open(ResourceLocation subject, String guide) {
        var scene = ResourceLocation.parse("bertieemi:unbound/" + guide);
        Minecraft.getInstance()
                .setScreen(new AssemblyScreen(PonderIndex.getSceneAccess().compile(subject).stream()
                        .filter(s -> scene.equals(s.getId()))
                        .toList()));
    }

    private static Guide forScene(net.createmod.ponder.foundation.PonderScene scene) {
        return guides.stream()
                .filter(g -> sceneId(g).equals(scene.getId()))
                .findFirst()
                .orElse(null);
    }

    private static void opening(net.neoforged.neoforge.client.event.ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof PonderUI ui
                && !(ui instanceof AssemblyScreen)
                && forScene(ui.getActiveScene()) != null) {
            var next = new AssemblyScreen(PonderIndex.getSceneAccess()
                    .compile(BuiltInRegistries.ITEM.getKey(ui.getSubject().getItem())));
            next.shareContextWith(ui);
            event.setNewScreen(next);
        }
    }

    public static class AssemblyScreen extends PonderUI {
        private List<ItemStack> steps = List.of();
        private ResourceLocation active;
        private final List<MaterialButton> buttons = new ArrayList<>();

        public AssemblyScreen(List<net.createmod.ponder.foundation.PonderScene> scenes) {
            super(scenes);
        }

        public int stepCount() {
            return steps.size();
        }

        public void seekStep(int index) {
            var scene = getActiveScene();
            seekToTime(scene.getKeyframeTime(scene.getKeyframeCount() - steps.size() + index) + 1);
        }

        private void refresh() {
            var scene = getActiveScene();
            if (scene.getId().equals(active)) return;
            active = scene.getId();
            var guide = forScene(scene);
            steps = guide == null ? List.of() : guide.materials();
            for (var button : buttons) removeWidget(button);
            buttons.clear();
            for (int i = 0; i < steps.size(); i++) {
                final int index = i;
                var button = new MaterialButton(
                        (width - steps.size() * 22) / 2 + i * 22, height - 22, steps.get(i), () -> seekStep(index));
                buttons.add(button);
                addWidget(button);
            }
        }

        @Override
        protected void init() {
            super.init();
            active = null;
            refresh();
        }

        @Override
        public boolean mouseClicked(double x, double y, int button) {
            for (var step : buttons) if (step.mouseClicked(x, y, button)) return true;
            return super.mouseClicked(x, y, button);
        }

        @Override
        protected void renderWidgets(GuiGraphics graphics, int x, int y, float partial) {
            refresh();
            super.renderWidgets(graphics, x, y, partial);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 500);
            for (var b : buttons) {
                b.render(graphics, x, y, partial);
                if (b.isMouseOver(x, y)) graphics.renderTooltip(font, b.item.getHoverName(), x, y);
            }
            graphics.pose().popPose();
        }
    }

    private static final class MaterialButton extends AbstractButton {
        private final ItemStack item;
        private final Runnable action;

        MaterialButton(int x, int y, ItemStack item, Runnable action) {
            super(x, y, 20, 20, item.getHoverName());
            this.item = item;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int x, int y, float delta) {
            g.blitSprite(
                    ResourceLocation.withDefaultNamespace(
                            isHoveredOrFocused() ? "widget/button_highlighted" : "widget/button"),
                    getX(),
                    getY(),
                    width,
                    height);
            g.renderItem(item, getX() + 2, getY() + 2);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
