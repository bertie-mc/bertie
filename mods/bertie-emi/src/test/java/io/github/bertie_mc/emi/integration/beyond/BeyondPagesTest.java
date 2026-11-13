package io.github.bertie_mc.emi.integration.beyond;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class BeyondPagesTest {
    @Test
    void everyPageHasUniqueIdentityAndFitsItsPanel() {
        var ids = new HashSet<String>();
        var groups = new HashSet<BeyondPages.Group>();
        for (var page : BeyondPages.all()) {
            assertTrue(ids.add(page.id()), page.id());
            assertFalse(page.nodes().isEmpty(), page.id());
            groups.add(page.group());
            for (var node : page.nodes()) {
                assertTrue(node.x() >= 0 && node.x() + 18 <= BeyondPages.WIDTH, page.id() + ": " + node);
                assertTrue(node.y() >= 12 && node.y() + 31 <= BeyondPages.HEIGHT, page.id() + ": " + node);
                assertFalse(node.hint().isBlank(), page.id());
            }
            for (var arrow : page.links()) {
                assertTrue(arrow.x1() >= 0 && arrow.x1() < BeyondPages.WIDTH, page.id());
                assertTrue(arrow.x2() >= 0 && arrow.x2() < BeyondPages.WIDTH, page.id());
                assertTrue(arrow.y1() >= 12 && arrow.y1() < BeyondPages.HEIGHT, page.id());
                assertTrue(arrow.y2() >= 12 && arrow.y2() < BeyondPages.HEIGHT, page.id());
            }
        }
        assertEquals(BeyondPages.Group.values().length, groups.size());
    }

    @Test
    void castingAlwaysShowsTheWholeBedIncludingItsCrackedCentre() {
        for (var cast : BeyondPages.CASTS) {
            var page = BeyondPages.casting(cast);
            var bed = page.nodes().stream()
                    .filter(node -> node.kind() == BeyondPages.Kind.BLOCK)
                    .toList();
            assertEquals(9, bed.size(), cast.tool());
            assertEquals(
                    9,
                    bed.stream()
                            .map(node -> List.of(node.x(), node.y()))
                            .distinct()
                            .count());
            assertTrue(bed.stream().allMatch(node -> node.role() == BeyondPages.Role.CATALYST));
            // A shovel's click target is not part of its solid outline, but must remain a block.
            var centre = bed.stream()
                    .filter(node -> node.x() == 32 && node.y() == 52)
                    .findFirst()
                    .orElseThrow();
            assertEquals("the_beyond:brittle_metal", centre.id());
            if (cast.tool().equals("shovel"))
                assertEquals("true", centre.properties().get("powered"));
            assertEquals(
                    1,
                    page.nodes().stream()
                            .filter(node -> node.role() == BeyondPages.Role.OUTPUT)
                            .count());
        }
    }

    @Test
    void eitherFlameIsOneAlternativeIngredientAndSheetsRemainProbabilistic() {
        var lighting = BeyondPages.all().stream()
                .filter(page -> page.id().equals("light_bonfire"))
                .findFirst()
                .orElseThrow();
        var inputs = lighting.nodes().stream()
                .filter(node -> node.role() == BeyondPages.Role.INPUT)
                .toList();
        assertEquals(1, inputs.size());
        assertEquals(BeyondPages.Kind.ALTERNATIVES, inputs.getFirst().kind());
        assertEquals(2, inputs.getFirst().id().split("\\|").length);
        var quenching = BeyondPages.all().stream()
                .filter(page -> page.id().equals("quenching"))
                .findFirst()
                .orElseThrow();
        var sheets = quenching.nodes().stream()
                .filter(node -> node.id().endsWith(":brittle_metal_sheet"))
                .findFirst()
                .orElseThrow();
        assertEquals(0.5f, sheets.chance());
    }

    @Test
    void sceneryDoesNotBecomeAnInventoryCostOrAnOutput() {
        var nodes = List.of(
                new BeyondPages.Node(
                        BeyondPages.Kind.ITEM,
                        "minecraft:coal",
                        1,
                        1,
                        BeyondPages.Role.INPUT,
                        10,
                        20,
                        "",
                        "input",
                        Map.of()),
                new BeyondPages.Node(
                        BeyondPages.Kind.BLOCK,
                        "minecraft:furnace",
                        1,
                        1,
                        BeyondPages.Role.CATALYST,
                        40,
                        20,
                        "",
                        "reused",
                        Map.of()),
                new BeyondPages.Node(
                        BeyondPages.Kind.ITEM,
                        "minecraft:stone",
                        1,
                        0.5f,
                        BeyondPages.Role.OUTPUT,
                        70,
                        20,
                        "",
                        "result",
                        Map.of()),
                new BeyondPages.Node(
                        BeyondPages.Kind.BLOCK,
                        "minecraft:water",
                        1,
                        1,
                        BeyondPages.Role.DISPLAY,
                        100,
                        20,
                        "",
                        "context",
                        Map.of()));
        var page = new BeyondPages.Page("fixture", BeyondPages.Group.METAL, "Fixture", nodes, List.of(), List.of());
        var recipe = new BeyondSchematicRecipe(null, ResourceLocation.parse("bertieemi:beyond/fixture"), page);
        assertEquals(1, recipe.getInputs().size());
        assertEquals(1, recipe.getCatalysts().size());
        assertEquals(1, recipe.getOutputs().size());
        assertEquals(0.5f, recipe.getOutputs().getFirst().getChance());
        assertFalse(recipe.supportsRecipeTree());
    }
}
