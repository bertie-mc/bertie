package io.github.bertie_mc.fletching;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class CoatingColorTest {
    @Test
    void colorsWithAlphaProduceValidRgbResourceNames() {
        for (int color : new int[] {0xff8bafe0, 0x8bafe0, 0, -1, 0x80123456}) {
            String token = CoatingColor.encode(color);
            assertNotNull(ResourceLocation.tryBuild("fletchery_expanded", "item/arrow_layers/effect_potion_" + token));
            assertEquals(color & 0xffffff, CoatingColor.decode(token).orElseThrow());
        }
        assertEquals(CoatingColor.encode(0xff8bafe0), CoatingColor.encode(0x8bafe0));
    }

    @Test
    void oldArrowColorsRemainReadableAndMalformedMarkersStayIntercepted() {
        assertEquals(0x8bafe0, CoatingColor.decode("#ff8bafe0").orElseThrow());
        assertEquals(0x8bafe0, CoatingColor.decode("#8bafe0").orElseThrow());
        assertEquals(0xffffff, CoatingColor.decode("#ffffffff").orElseThrow());
        assertTrue(CoatingColor.isEncoded("#bad-color"));
        assertTrue(CoatingColor.decode("#bad-color").isEmpty());
        assertFalse(CoatingColor.isEncoded("poison"));
    }
}
