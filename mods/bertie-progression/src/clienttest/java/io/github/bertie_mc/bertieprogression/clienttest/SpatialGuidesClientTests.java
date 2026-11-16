package io.github.bertie_mc.bertieprogression.clienttest;

import com.google.gson.JsonParser;
import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import java.io.InputStreamReader;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class SpatialGuidesClientTests {
    @ClientTest
    public static void allGuidesCompileAndSouthFacingPreviewRenders(ClientTestContext context) throws Exception {
        try (var world = context.worldBuilder().create()) {
            var guides = JsonParser.parseReader(new InputStreamReader(
                            io.github.bertie_mc.bertieprogression.ponder.SpatialPonderPlugin.class.getResourceAsStream(
                                    "/spatial_guides.json")))
                    .getAsJsonArray();
            context.runOnClient(client -> {
                if (guides.size() != 15)
                    throw new AssertionError("Thirteen Mini guides and two Giant Anvil guides required");
                for (var entry : guides) {
                    var id = ResourceLocation.parse(
                            entry.getAsJsonObject().get("recipe").getAsString());
                    var scenes = io.github.bertie_mc.bertieprogression.ponder.SpatialPonderPlugin.scenes(id);
                    if (scenes.size() != 1) throw new AssertionError("Missing individual recipe guide: " + id);
                    var scene = scenes.getFirst();
                    if (scene.getTitle().contains(".ponder."))
                        throw new AssertionError("Untranslated guide title: " + id);
                    scene.seekToTime(scene.getTotalTime());
                }
                var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("bertieprogression:sentient_casing"));
                var model = client.getBlockRenderer().getBlockModel(block.defaultBlockState());
                if (model == client.getModelManager().getMissingModel())
                    throw new AssertionError("Sentient Casing model failed to bake");
                var recipeId = ResourceLocation.parse("bertieprogression:miniaturization/large_heat_generator");
                // Construct the real linked screen, then render through an unpaused fixture wrapper.
                io.github.bertie_mc.bertieprogression.ponder.SpatialPonderPlugin.screen(recipeId);
                var ui = new PreviewScreen(
                        io.github.bertie_mc.bertieprogression.ponder.SpatialPonderPlugin.scenes(recipeId));
                client.setScreen(ui);
                ui.seekToTime(310);
            });
            context.waitTicks(10);
            context.takeScreenshot("large-heat-mini-south-front");
            context.runOnClient(client -> {
                if (!(client.screen instanceof PonderUI ui)) throw new AssertionError("Ponder link did not open");
                float angle = ui.getActiveScene().getTransform().yRotation.getValue(1);
                if (Math.abs(angle % 360) > 0.1f)
                    throw new AssertionError("Preview must face the south side: " + angle);
                client.setScreen(null);
            });
        }
    }

    private static final class PreviewScreen extends PonderUI {
        private PreviewScreen(java.util.List<net.createmod.ponder.foundation.PonderScene> scenes) {
            super(scenes);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
