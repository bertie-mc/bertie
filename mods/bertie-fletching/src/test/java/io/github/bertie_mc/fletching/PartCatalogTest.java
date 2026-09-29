package io.github.bertie_mc.fletching;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class PartCatalogTest {
    @Test
    void requestedOrderAndCountsArePreserved() {
        assertEquals(
                List.of(
                        "wheat",
                        "feather",
                        "emu",
                        "raven",
                        "roadrunner",
                        "phantom",
                        "stymphalian",
                        "amphithere",
                        "resplendent",
                        "sun",
                        "resonant",
                        "neutronium"),
                PartCatalog.inSlot(0).stream().map(PartCatalog.Part::key).toList());
        assertEquals(
                List.of(
                        "stick",
                        "leather",
                        "fishing",
                        "chain",
                        "bone",
                        "breeze",
                        "dark",
                        "blaze",
                        "end",
                        "experience",
                        "infinity",
                        "dielectric",
                        "ender"),
                PartCatalog.inSlot(1).stream().map(PartCatalog.Part::key).toList());
        assertEquals(22, PartCatalog.inSlot(2).size());
        assertEquals(18, PartCatalog.inSlot(3).size());
        for (String removed : List.of(
                "gunpowder",
                "glowstone_dust",
                "turtle_helmet",
                "heart_of_the_sea",
                "fire_charge",
                "firework_star",
                "firework_rocket")) assertNull(PartCatalog.find(3, "minecraft:" + removed));
        assertNull(PartCatalog.find(2, "minecraft:shulker_shell"));
    }

    @Test
    void everyPartHasTagsDescriptionsAndAllRenderLayers() throws Exception {
        for (var part : PartCatalog.ALL) {
            String category = List.of("feather", "shaft", "tip", "extra").get(part.slot());
            try (var tag = getClass().getResourceAsStream("/data/fletchery/tags/item/" + category + ".json")) {
                assertNotNull(tag);
                assertTrue(new String(tag.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                        .contains(part.itemId()));
            }
            try (var lang = getClass().getResourceAsStream("/assets/bertiefletching/lang/en_us.json")) {
                assertTrue(new String(lang.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                        .contains(part.translation()));
            }
            String prefix = part.slot() == 3 ? "effect" : category;
            for (String kind : List.of("arrow", "crossbow", "bow_0", "bow_1", "bow_2")) {
                String folder = kind.startsWith("bow") ? "bow_layers" : kind + "_layers";
                String suffix = kind.startsWith("bow") ? kind.substring(3) : "";
                String resource = "/assets/fletchery_expanded/models/__layer_stitch/item_" + folder + "_" + prefix + "_"
                        + part.visual() + suffix + ".json";
                assertNotNull(com.fletchery.mod.FletcheryExpanded.class.getResource(resource), resource);
                try (var stream = com.fletchery.mod.FletcheryExpanded.class.getResourceAsStream(resource)) {
                    var json = com.google.gson.JsonParser.parseReader(
                                    new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8))
                            .getAsJsonObject();
                    String texture =
                            json.getAsJsonObject("textures").get("layer0").getAsString();
                    String[] id = texture.split(":", 2);
                    assertNotNull(
                            com.fletchery.mod.FletcheryExpanded.class.getResource(
                                    "/assets/" + id[0] + "/textures/" + id[1] + ".png"),
                            texture);
                }
            }
        }
    }

    @Test
    void launchHeightUsesFiniteBallisticVelocity() {
        double speed = ArrowDamage.launchSpeed(7), height = 0;
        for (int i = 0; i < 200 && speed > 0; i++) {
            height += speed;
            speed = (speed - .08) * .98;
        }
        assertEquals(7, height, .00001);
    }
}
