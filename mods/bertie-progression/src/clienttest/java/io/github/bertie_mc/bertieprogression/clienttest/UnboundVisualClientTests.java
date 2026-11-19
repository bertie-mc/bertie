package io.github.bertie_mc.bertieprogression.clienttest;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.widget.Widget;
import io.github.bertie_mc.emi.integration.unbound.UnboundPonders;
import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import java.util.List;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public final class UnboundVisualClientTests {
    @ClientTest
    public static void placedAdapterAndUnboundAssemblyGuidesRender(ClientTestContext context) throws Exception {
        try (var world = context.worldBuilder().create()) {
            world.server()
                    .runCommand(
                            "setblock 0 64 0 bertieprogression:mechanical_adapter[facing=north,half=lower,machine_assembled=true]");
            world.server()
                    .runCommand(
                            "setblock 0 65 0 bertieprogression:mechanical_adapter[facing=north,half=upper,machine_assembled=true]");
            world.server().runCommand("tp @a 3.5 65.5 4.5 140 10");
            world.server().runCommand("time set noon");
            world.connection().waitForClientboundPackets();
            context.runOnClient(client -> {
                for (var repair : new String[][] {
                    {"boarding_hook", "bertieprogression:iron_sheet"},
                    {"crater_carapace_helmet", "anvilcraft:titanium_ingot"}
                }) {
                    var gear = BuiltInRegistries.ITEM.get(ResourceLocation.parse("unbound:" + repair[0]));
                    var material = BuiltInRegistries.ITEM.get(ResourceLocation.parse(repair[1]));
                    if (!gear.isValidRepairItem(
                            new net.minecraft.world.item.ItemStack(gear),
                            new net.minecraft.world.item.ItemStack(material)))
                        throw new AssertionError("Merged repair material is unavailable for " + repair[0]);
                }
                var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("bertieprogression:mechanical_adapter"));
                for (var state : block.getStateDefinition().getPossibleStates()) {
                    var model = client.getBlockRenderer().getBlockModel(state);
                    if (model == client.getModelManager().getMissingModel()
                            || model.getQuads(state, null, RandomSource.create(0))
                                            .size()
                                    < 20) throw new AssertionError("Adapter placed model is a fallback cube: " + state);
                }
                if (UnboundPonders.guides().size() != 8)
                    throw new AssertionError("Missing native rocket/emitter guides");
                for (var guide : UnboundPonders.guides()) {
                    var ui = screen(guide);
                    client.setScreen(ui);
                    if (ui.stepCount() != guide.materials().size())
                        throw new AssertionError("Missing assembly material controls");
                    ui.seekStep(ui.stepCount() - 1);
                    for (var cell : guide.cells().entrySet())
                        if (!ui.getActiveScene()
                                .getWorld()
                                .getBlockState(cell.getKey())
                                .equals(cell.getValue()))
                            throw new AssertionError("Missing assembly block: " + guide.id() + " " + cell.getKey());
                    if (ui.getActiveScene().getTitle().startsWith("bertieemi.ponder."))
                        throw new AssertionError("Untranslated assembly title: " + guide.id());
                    float end = ui.getActiveScene().getSceneProgress();
                    ui.seekStep(0);
                    if (ui.stepCount() > 1 && ui.getActiveScene().getSceneProgress() >= end)
                        throw new AssertionError("Assembly controls do not seek backwards");
                }
                client.setScreen(null);
                client.options.hideGui = true;
            });
            context.waitTicks(8);
            context.takeScreenshot("unbound-fixed-mechanical-adapter");
            context.runOnClient(client -> {
                try {
                    var type =
                            Class.forName("io.github.bertie_mc.emi.integration.unbound.UnboundEmiModule$PonderButton");
                    var ctor = type.getDeclaredConstructor(int.class, int.class, ResourceLocation.class, String.class);
                    ctor.setAccessible(true);
                    var button =
                            (Widget) ctor.newInstance(2, 2, ResourceLocation.parse("unbound:launch_pad"), "launch_pad");
                    if (!button.mouseClicked(6, 6, 0)
                            || !(client.screen instanceof PonderUI ui)
                            || !ui.getActiveScene().getId().toString().equals("bertieemi:unbound/launch_pad"))
                        throw new AssertionError("EMI Ponder button opened the wrong assembly");
                } catch (ReflectiveOperationException failure) {
                    throw new IllegalStateException(failure);
                }
                client.options.hideGui = false;
                var lunar = UnboundPonders.guides().stream()
                        .filter(g -> g.id().equals("lunar"))
                        .findFirst()
                        .orElseThrow();
                var ui = screen(lunar);
                client.setScreen(ui);
                ui.seekStep(ui.stepCount() - 1);
                var buttons = ui.children().stream()
                        .filter(AbstractButton.class::isInstance)
                        .map(AbstractButton.class::cast)
                        .toList();
                if (buttons.size() != ui.stepCount())
                    throw new AssertionError("Material strip did not create all buttons");
            });
            context.waitTicks(3);
            context.takeScreenshot("unbound-lunar-assembly-materials");
            context.runOnClient(client -> {
                var emitter = UnboundPonders.guides().stream()
                        .filter(g -> g.id().equals("emitter"))
                        .findFirst()
                        .orElseThrow();
                var ui = screen(emitter);
                client.setScreen(ui);
                ui.seekStep(ui.stepCount() - 1);
            });
            context.waitTicks(3);
            context.takeScreenshot("unbound-emitter-assembly-materials");
            context.runOnClient(client -> client.setScreen(null));
            boolean[] ready = {false};
            for (int attempt = 0; attempt < 20 && !ready[0]; attempt++) {
                context.runOnClient(client -> ready[0] = EmiApi.getRecipeManager().getRecipes().stream()
                        .anyMatch(
                                r -> r.getId() != null && r.getId().toString().startsWith("bertieemi:unbound/fuel/")));
                if (!ready[0]) context.waitTicks(10);
            }
            if (!ready[0]) throw new AssertionError("Unbound EMI integration did not index fuel recipes");
        }
    }

    private static UnpausedGuide screen(UnboundPonders.Guide guide) {
        var id = ResourceLocation.parse("bertieemi:unbound/" + guide.id());
        return new UnpausedGuide(PonderIndex.getSceneAccess().compile(guide.subject()).stream()
                .filter(s -> id.equals(s.getId()))
                .toList());
    }

    private static final class UnpausedGuide extends UnboundPonders.AssemblyScreen {
        UnpausedGuide(List<PonderScene> scenes) {
            super(scenes);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private UnboundVisualClientTests() {}
}
