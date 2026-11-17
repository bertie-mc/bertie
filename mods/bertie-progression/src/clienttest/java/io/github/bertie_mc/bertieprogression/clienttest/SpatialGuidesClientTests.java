package io.github.bertie_mc.bertieprogression.clienttest;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.bertie_mc.bertieprogression.emi.SpatialPonderButton;
import io.github.bertie_mc.bertieprogression.ponder.SpatialPonderPlugin;
import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import java.io.InputStreamReader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class SpatialGuidesClientTests {
    private static final ResourceLocation RECIPE =
            ResourceLocation.parse("bertieprogression:miniaturization/large_heat_generator");

    @ClientTest
    public static void queuedRecipesLoadAtNativeStations(ClientTestContext context) throws Exception {
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(client -> {
                var server = client.getSingleplayerServer();
                var recipes = server.getRecipeManager();
                var access = server.registryAccess();
                for (String id : java.util.List.of(
                        "irons_spellbooks:arcane_anvil",
                        "mekanism:chemical_oxidizer",
                        "mekanism:quantum_entangloporter",
                        "mekanism:pigment_mixer",
                        "mekanism:pigment_extractor",
                        "mekanism_extras:reaction/molten_thermonuclear_gasification/molten_thermonuclear")) {
                    if (recipes.byKey(ResourceLocation.parse(id)).isEmpty())
                        throw new AssertionError("Selected recipe failed native loading: " + id);
                }
                var anvil = recipes.byKey(ResourceLocation.parse("irons_spellbooks:arcane_anvil"))
                        .orElseThrow()
                        .value();
                if (!BuiltInRegistries.RECIPE_TYPE
                        .getKey(anvil.getType())
                        .toString()
                        .equals("malum:spirit_infusion"))
                    throw new AssertionError("Arcane Anvil belongs at the Spirit Altar");
                var quantum = recipes.byKey(ResourceLocation.parse("mekanism:quantum_entangloporter"))
                        .orElseThrow()
                        .value();
                if (quantum.getResultItem(access).getCount() != 2)
                    throw new AssertionError("Entangloporter output must be two");
                var red = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("mekanism:alloy_infused")));
                var thermo = new ItemStack(
                        BuiltInRegistries.ITEM.get(ResourceLocation.parse("mekanism_extras:alloy_thermonuclear")));
                for (String name : java.util.List.of("pigment_mixer", "pigment_extractor")) {
                    var recipe = recipes.byKey(ResourceLocation.parse("mekanism:" + name))
                            .orElseThrow()
                            .value();
                    if (recipe.getIngredients().stream().noneMatch(i -> i.test(red))
                            || recipe.getIngredients().stream().anyMatch(i -> i.test(thermo)))
                        throw new AssertionError("Pigment machine must use the red alloy: " + name);
                }
                for (String name : java.util.List.of(
                        "heat_generator",
                        "gas_burning_generator",
                        "pigment_mixer",
                        "chemical_infuser",
                        "electrolytic_separator")) {
                    if (recipes.byKey(ResourceLocation.parse("mekmm:large_" + name))
                            .isPresent())
                        throw new AssertionError("Large Mini machine must not retain its crafting shortcut: " + name);
                }
            });
        }
    }

    @ClientTest
    public static void linkedButtonStepSeekingTooltipsAndFlatCasingRender(ClientTestContext context) throws Exception {
        try (var world = context.worldBuilder().create()) {
            var guides = JsonParser.parseReader(new InputStreamReader(
                            SpatialPonderPlugin.class.getResourceAsStream("/spatial_guides.json")))
                    .getAsJsonArray();
            double[] cursor = new double[2];
            context.runOnClient(client -> {
                if (guides.size() != 15) throw new AssertionError("All fifteen spatial guides remain available");
                for (var entry : guides) {
                    var id = ResourceLocation.parse(
                            entry.getAsJsonObject().get("recipe").getAsString());
                    var ui = (SpatialPonderPlugin.RecipeScreen) SpatialPonderPlugin.screen(id);
                    var scene = ui.getActiveScene();
                    int extra = scene.getKeyframeCount() - ui.stepCount();
                    if (extra < 0 || extra > 1)
                        throw new AssertionError("Every material phase must have a seek marker: " + id + " markers="
                                + scene.getKeyframeCount() + " steps=" + ui.stepCount());
                    scene.seekToTime(scene.getTotalTime());
                }
                var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("bertieprogression:sentient_casing"));
                if (client.getBlockRenderer().getBlockModel(block.defaultBlockState())
                        == client.getModelManager().getMissingModel())
                    throw new AssertionError("Casing failed to bake");
                var subject =
                        new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("mekmm:large_heat_generator")));
                client.setScreen(net.createmod.ponder.foundation.ui.PonderUI.of(subject));
                if (!(client.screen instanceof SpatialPonderPlugin.RecipeScreen))
                    throw new AssertionError("Normal Ponder shortcut must expose the material-step controls too");
                var screen = new ButtonPreview();
                client.setScreen(screen);
                var bounds = screen.button.getBounds();
                cursor[0] = (bounds.x() + 10) * client.getWindow().getGuiScale();
                cursor[1] = (bounds.y() + 8) * client.getWindow().getGuiScale();
            });
            context.input().setCursorPos(cursor[0], cursor[1]);
            context.waitTicks(3);
            context.takeScreenshot("spatial-emi-button-and-flat-casing");
            var mouse = InputConstants.Type.MOUSE.getOrCreate(0);
            context.input().pressKey(mouse);
            context.runOnClient(client -> {
                if (!(client.screen instanceof SpatialPonderPlugin.RecipeScreen))
                    throw new AssertionError("Clicking the EMI widget must open its real recipe guide");
                // The driver waits on server ticks; only the pause policy differs in this fixture.
                var ui = new UnpausedGuide();
                client.setScreen(ui);
                var buttons = ui.children().stream()
                        .filter(AbstractButton.class::isInstance)
                        .map(AbstractButton.class::cast)
                        .toList();
                if (buttons.size() != ui.stepCount()) throw new AssertionError("Missing material buttons");
                var last = buttons.getLast();
                ui.mouseClicked(last.getX() + 8, last.getY() + 8, 0);
                float end = ui.getActiveScene().getSceneProgress();
                var first = buttons.getFirst();
                var hovered = ui.stepItemAt(first.getX() + 8, first.getY() + 8);
                if (hovered.isEmpty()
                        || !hovered.getHoverName()
                                .getString()
                                .equals(first.getMessage().getString()))
                    throw new AssertionError("Hovering a step must expose its material item");
                ui.mouseClicked(first.getX() + 8, first.getY() + 8, 0);
                if (ui.getActiveScene().getSceneProgress() >= end)
                    throw new AssertionError("Step controls must seek backwards as well as forwards");
                ui.seekStep(ui.stepCount() - 3);
                float angle = ui.getActiveScene().getTransform().yRotation.getValue(1);
                if (Math.abs(angle + 30) > 0.1f) throw new AssertionError("Preview must be angled: " + angle);
                var middle = buttons.get(buttons.size() / 2);
                cursor[0] = (middle.getX() + 8) * client.getWindow().getGuiScale();
                cursor[1] = (middle.getY() + 8) * client.getWindow().getGuiScale();
            });
            context.input().releaseKey(mouse);
            context.input().setCursorPos(cursor[0], cursor[1]);
            context.waitTicks(5);
            context.takeScreenshot("spatial-material-steps-angled");
            context.runOnClient(client -> client.setScreen(null));
        }
    }

    private static final class UnpausedGuide extends SpatialPonderPlugin.RecipeScreen {
        private UnpausedGuide() {
            super(RECIPE);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private static final class ButtonPreview extends Screen {
        private SpatialPonderButton button;

        private ButtonPreview() {
            super(Component.literal("Spatial controls"));
        }

        @Override
        protected void init() {
            button = new SpatialPonderButton(width / 2 - 29, height / 2 - 30, RECIPE);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public boolean mouseClicked(double x, double y, int mouseButton) {
            return button.mouseClicked((int) x, (int) y, mouseButton) || super.mouseClicked(x, y, mouseButton);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            graphics.fill(0, 0, width, height, 0xFF30333C);
            button.render(graphics, mouseX, mouseY, delta);
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("bertieprogression:sentient_casing"));
            graphics.pose().pushPose();
            graphics.pose().translate(width / 2.0 - 32, height / 2.0 + 8, 0);
            graphics.pose().scale(4, 4, 4);
            graphics.renderItem(new ItemStack(item), 0, 0);
            graphics.pose().popPose();
        }
    }
}
