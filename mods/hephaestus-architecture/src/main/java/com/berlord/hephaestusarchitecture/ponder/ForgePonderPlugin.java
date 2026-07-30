package com.berlord.hephaestusarchitecture.ponder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Ponder scenes for the tier 2-4 Hephaestus Forge layouts.
 *
 * <p>Forbidden &amp; Arcanus ships its own ponder for the native forge, but it is a baked
 * {@code assets/forbidden_arcanus/ponder/hephaestus_forge.nbt} plus a hardcoded scene script — it
 * knows nothing about this addon and keeps teaching the stock 9x9 for every tier. These scenes fill
 * that gap.
 *
 * <p>Each scene's schematic is THE SAME NBT the matcher validates against, copied from
 * {@code data/hephaestusarchitecture/structure/hephaestus_forge/} into
 * {@code assets/hephaestusarchitecture/ponder/}. A scene therefore cannot teach a layout
 * {@link com.berlord.hephaestusarchitecture.structure.StructureTemplateLayoutLoader} would reject.
 *
 * <p>Entirely client-side, and only loaded when Ponder is present (see {@code ClientSetup}).
 */
public class ForgePonderPlugin implements PonderPlugin {

    private static final String MODID = "hephaestusarchitecture";
    private static final String FA = "forbidden_arcanus";

    /** The craftable forge. All three scenes hang off this so players actually find them. */
    private static final ResourceLocation BASE_FORGE = fa("hephaestus_forge_tier_1");

    public static void register() {
        PonderIndex.addPlugin(new ForgePonderPlugin());
    }

    @Override
    public String getModId() {
        return MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        for (Tier tier : TIERS) {
            ResourceLocation schematic = ResourceLocation.fromNamespaceAndPath(MODID, tier.id());
            helper.addStoryBoard(BASE_FORGE, schematic, tier::build);
            helper.addStoryBoard(fa("hephaestus_forge_" + tier.id()), schematic, tier::build);
        }
    }

    private static ResourceLocation fa(String path) {
        return ResourceLocation.fromNamespaceAndPath(FA, path);
    }

    // ---------------------------------------------------------------------------------------------
    // Scene definitions
    // ---------------------------------------------------------------------------------------------

    /**
     * One revealed step of a build. {@code fromLayer}/{@code toLayer} are inclusive Y indices into
     * the schematic; {@code pointY} is the Y the caption arrow points at.
     */
    private record Step(int fromLayer, int toLayer, int pointX, int pointY, int pointZ,
                        PonderPalette color, String text) {

        static Step of(int layer, int px, int pz, String text) {
            return new Step(layer, layer, px, layer, pz, null, text);
        }

        static Step of(int from, int to, int px, int pz, PonderPalette color, String text) {
            return new Step(from, to, px, to, pz, color, text);
        }
    }

    /**
     * A tier scene. {@code scale}/{@code offsetY} are pure framing: these builds are far larger than
     * anything Create ponders (most of its scenes are 5x2..4), so each one is scaled down and pushed
     * down the screen. NEGATIVE offsetY moves the scene DOWN.
     */
    private record Tier(String id, String title, int size, float scale, float offsetY,
                        int pedestals, List<Step> steps) {

        void build(SceneBuilder scene, SceneBuildingUtil util) {
            scene.title(id, title);
            scene.configureBasePlate(0, 0, size);
            scene.scaleSceneView(scale);
            scene.setSceneOffsetY(offsetY);
            scene.showBasePlate();
            scene.idle(10);

            int centre = size / 2;

            for (Step step : steps) {
                if (step.fromLayer() > 0) {
                    scene.world().showSection(
                            util.select().fromTo(0, step.fromLayer(), 0, size - 1, step.toLayer(), size - 1),
                            Direction.DOWN);
                    scene.idle(15);
                }

                var text = scene.overlay().showText(90)
                        .text(step.text())
                        .placeNearTarget()
                        .attachKeyFrame()
                        .pointAt(util.vector().topOf(step.pointX(), step.pointY(), step.pointZ()));
                if (step.color() != null) {
                    text.colored(step.color());
                }
                scene.idle(100);
            }

            scene.overlay().showText(90)
                    .colored(PonderPalette.INPUT)
                    .text(pedestals + " pedestals feed this forge. Any pedestal counts - plain or magnetized.")
                    .placeNearTarget()
                    .attachKeyFrame()
                    .pointAt(util.vector().topOf(centre, 1, 0));
            scene.idle(100);

            scene.overlay().showText(90)
                    .colored(PonderPalette.OUTPUT)
                    .text("Build it in any of the four rotations. Mirror images are NOT accepted.")
                    .placeNearTarget()
                    .attachKeyFrame()
                    .pointAt(util.vector().topOf(centre, 1, centre));
            scene.idle(100);
            scene.markAsFinished();
        }
    }

    private static final List<Tier> TIERS = List.of(
            new Tier("tier_2", "Hephaestus Forge - Tier 2 Layout", 9, 0.75f, -0.5f, 8, List.of(
                    Step.of(0, 4, 4,
                            "Tier 2 replaces the stock base entirely. Nine by nine of Polished Darkstone with the corners cut away."),
                    Step.of(0, 0, 4, 0, PonderPalette.BLUE,
                            "Soulstone on the diagonals, Gilded Chiseled Darkstone on the arms, and Chiseled Resin Bricks at the four points."),
                    Step.of(1, 4, 4,
                            "The forge sits dead centre, ringed by eight pedestals and four corner stairs."),
                    Step.of(2, 2, 1, 1, PonderPalette.OUTPUT,
                            "Cap the four corners with Arcane Crystal Blocks."))),

            new Tier("tier_3", "Hephaestus Forge - Tier 3 Layout", 13, 0.55f, -1.0f, 12, List.of(
                    Step.of(0, 6, 6,
                            "Tier 3 widens to a thirteen by thirteen octagon, edged all the way round with Darkstone Slabs."),
                    Step.of(0, 0, 6, 0, PonderPalette.RED,
                            "Blocks of Living Flesh mark the inner ring, with Arcane Polished Darkstone threading between them."),
                    Step.of(1, 6, 6,
                            "The forge at centre, now with twelve pedestals on a wider ring - four more inputs than tier 2."),
                    Step.of(2, 4, 3, 3, PonderPalette.OUTPUT,
                            "Four pillars rise at the diagonals: Gilded Chiseled Darkstone, an Arcane Crystal Block, then Gilded again."))),

            new Tier("tier_4", "Hephaestus Forge - Tier 4 Layout", 13, 0.5f, -1.5f, 12, List.of(
                    Step.of(0, 6, 6,
                            "Tier 4 keeps the thirteen by thirteen footprint but inlays it with Frost Infused Netherite."),
                    Step.of(0, 0, 6, 6, PonderPalette.RED,
                            "Voiderite and Stellarite sit at the heart of the floor, directly under the forge."),
                    Step.of(1, 6, 6,
                            "Twelve pedestals again - tier 4 buys you a harder build and taller structure, not more inputs."),
                    Step.of(2, 4, 3, 3, PonderPalette.BLUE,
                            "Neolith Blocks stack into four pillars at the diagonals. Watch their facing - up, up, down."),
                    Step.of(5, 6, 3, 3, PonderPalette.OUTPUT,
                            "Crown the pillars with a canopy of upside-down stairs and slabs."))));
}
