package io.github.bertie_mc.fletching.client;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.bertie_mc.fletching.PartCatalog;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MaterialLayersTest {
    @Test
    void ColorRemappingReplacesOriginalHueAndPreservesTransparency() {
        try (var image = new NativeImage(3, 1, true)) {
            image.setPixelRGBA(0, 0, 0xff000040);
            image.setPixelRGBA(1, 0, 0xff0000ff);
            MaterialLayers.recolor(image, new int[] {0x000040, 0x000080, 0x0000ff});
            assertEquals(0xff400000, image.getPixelRGBA(0, 0));
            assertEquals(0xffff0000, image.getPixelRGBA(1, 0));
            assertEquals(0, image.getPixelRGBA(2, 0));
        }
    }

    @Test
    void EveryCustomMaterialHasAllBakedViews() throws Exception {
        for (var part : PartCatalog.ALL)
            if (part.tint() != -1)
                for (String view : MaterialLayers.VIEWS) {
                    String path = "/assets/bertiefletching/models/item/parts/" + part.slot() + "_" + part.key() + "/"
                            + view + ".json";
                    try (var stream = getClass().getResourceAsStream(path)) {
                        assertNotNull(stream, path);
                        assertTrue(new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                                .contains(MaterialLayers.id(part, view).toString()));
                    }
                }
    }

    @Test
    void DescriptionsAreValidUtf8AndRemovedMaterialsAreRejected() throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/bertiefletching/lang/en_us.json")) {
            var json = com.google.gson.JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            assertEquals(
                    "2\u00d7 XP.", json.get("bertiefletching.part.1.experience").getAsString());
            assertEquals(
                    "+20% arrow speed, -20% gravity.",
                    json.get("bertiefletching.part.0.emu").getAsString());
        }
        assertNull(PartCatalog.find(1, "betterend:leather_wrapped_stick"));
        assertNull(PartCatalog.find(2, "born_in_chaos_v1:permafrost_shard"));
        assertNotNull(PartCatalog.find(2, "irons_spellbooks:permafrost_shard"));
    }
}
