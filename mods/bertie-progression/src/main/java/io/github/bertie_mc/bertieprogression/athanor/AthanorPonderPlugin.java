package io.github.bertie_mc.bertieprogression.athanor;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Ponder scene for Magitech's Athanor Altar, attached to the Athanor Pillar — hover it and press W.
 *
 * <p>The pillar validates the whole {@code magitech:athanor_pillar_altar} multiblock and, when it
 * fails, highlights only the FIRST mismatched position. That is a one-block-at-a-time error marker,
 * not an instruction, and it reads as "put something here" — which is exactly the wrong idea. This
 * scene shows the finished altar instead, one layer at a time.
 *
 * <p>Layout facts are read out of the shipped structure, not guessed: 9x7x9, pillar dead centre at
 * (4,1,4), twelve Alchemetric Pylons in three rings, five Mana Vessels and five Mana Nodes. The
 * pylon rings correspond one-to-one with the three ingredient rows of an
 * {@code athanor_pillar_infusion} recipe (verified against {@code AthanorPillarBlockEntity}'s
 * {@code getPylonPos}, which is a flat switch indexed {@code 4 * row + slot}).
 *
 * <p>Entirely client-side, and only loaded when Ponder is present (see {@code ClientSetup}).
 */
public class AthanorPonderPlugin implements PonderPlugin {

    private static final String MODID = "bertieprogression";
    /** assets/bertieprogression/ponder/athanor_altar.nbt */
    private static final ResourceLocation SCHEMATIC = ResourceLocation.fromNamespaceAndPath(MODID, "athanor_altar");

    private static final ResourceLocation PILLAR = ResourceLocation.fromNamespaceAndPath("magitech", "athanor_pillar");

    public static void register() {
        PonderIndex.addPlugin(new AthanorPonderPlugin());
    }

    @Override
    public String getModId() {
        return MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(PILLAR, SCHEMATIC, AthanorPonderPlugin::athanorScene);
    }

    private static void athanorScene(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("athanor_altar", "Building the Athanor Altar");
        scene.configureBasePlate(0, 0, 9);
        // 9x9x7 is larger again than the Deep Waters Shrine (7x7x6 at 0.65), so scale down further
        // and push the scene down the screen. NEGATIVE offsetY moves it DOWN.
        scene.scaleSceneView(0.5f);
        scene.setSceneOffsetY(-2.0f);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos pillar = util.grid().at(4, 1, 4);

        // --- the floor --------------------------------------------------------------------------
        scene.overlay()
                .showText(90)
                .text("Nine by nine of Alchecrysite - polished, bricks, stairs, with Fluorite Bricks set in.")
                .placeNearTarget()
                .attachKeyFrame()
                .pointAt(util.vector().topOf(4, 0, 4));
        scene.idle(100);

        // --- y=1: the pillar, first pylon ring, vessels ------------------------------------------
        scene.world().showSection(util.select().layer(1), Direction.DOWN);
        scene.idle(15);
        scene.overlay()
                .showText(100)
                .colored(PonderPalette.OUTPUT)
                .text(
                        "The Athanor Pillar stands dead centre. The substrate goes on TOP of it - the frame you are upgrading.")
                .placeNearTarget()
                .attachKeyFrame()
                .pointAt(util.vector().topOf(pillar));
        scene.idle(110);
        scene.overlay()
                .showText(100)
                .colored(PonderPalette.INPUT)
                .text(
                        "Four Alchemetric Pylons on the diagonals, two out. These hold MATERIALS, one item each - not the pillar.")
                .placeNearTarget()
                .attachKeyFrame()
                .pointAt(util.vector().topOf(2, 1, 2));
        scene.idle(110);
        scene.overlay()
                .showText(90)
                .colored(PonderPalette.BLUE)
                .text("Mana Vessels sit in the four corners, three out on each diagonal.")
                .placeNearTarget()
                .attachKeyFrame()
                .pointAt(util.vector().topOf(1, 1, 1));
        scene.idle(100);

        // --- y=2: mana nodes ---------------------------------------------------------------------
        scene.world().showSection(util.select().layer(2), Direction.DOWN);
        scene.idle(15);
        scene.overlay()
                .showText(80)
                .colored(PonderPalette.BLUE)
                .text("A Mana Node caps each vessel.")
                .placeNearTarget()
                .attachKeyFrame()
                .pointAt(util.vector().topOf(1, 2, 1));
        scene.idle(90);

        // --- y=3: second pylon ring ---------------------------------------------------------------
        scene.world().showSection(util.select().layer(3), Direction.DOWN);
        scene.idle(15);
        scene.overlay()
                .showText(95)
                .colored(PonderPalette.INPUT)
                .text("The second ring of four pylons - on the axes this time, three out and two up.")
                .placeNearTarget()
                .attachKeyFrame()
                .pointAt(util.vector().topOf(4, 3, 1));
        scene.idle(105);

        // --- y=4: third pylon ring ----------------------------------------------------------------
        scene.world().showSection(util.select().layer(4), Direction.DOWN);
        scene.idle(15);
        scene.overlay()
                .showText(95)
                .colored(PonderPalette.INPUT)
                .text("The third ring sits on the far corners of the altar. Twelve pylons in total.")
                .placeNearTarget()
                .attachKeyFrame()
                .pointAt(util.vector().topOf(0, 4, 0));
        scene.idle(105);

        // --- y=5, y=6: the crown ------------------------------------------------------------------
        scene.world().showSection(util.select().layer(5), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().layer(6), Direction.DOWN);
        scene.idle(15);
        scene.overlay()
                .showText(90)
                .colored(PonderPalette.BLUE)
                .text("Cap the centre with a Mana Node and a Mana Vessel above the pillar, then close the roof.")
                .placeNearTarget()
                .attachKeyFrame()
                .pointAt(util.vector().topOf(4, 6, 4));
        scene.idle(100);

        // --- using it -----------------------------------------------------------------------------
        scene.addKeyframe();
        scene.overlay()
                .showText(120)
                .colored(PonderPalette.OUTPUT)
                .text(
                        "Substrate on the pillar, one material on each of the twelve pylons. The recipe's three ingredient rows are the three rings, innermost first.")
                .placeNearTarget()
                .attachKeyFrame()
                .pointAt(util.vector().topOf(pillar));
        scene.idle(130);
        scene.overlay()
                .showText(100)
                .text(
                        "Boots need 500 mana. Until every block matches, the pillar marks ONE wrong position in red at a time.")
                .placeNearTarget()
                .attachKeyFrame()
                .pointAt(util.vector().topOf(4, 0, 4));
        scene.idle(110);
        scene.markAsFinished();
    }
}
