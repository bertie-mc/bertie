package io.github.bertie_mc.screenshotcopy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class RgbaPixelsTest {
    @Test
    void convertsNativeImageAbgrPixelsToRgbaBytes() {
        int[] pixels = {
            0x44332211,
            0x8000FF7F,
        };

        byte[] rgba = RgbaPixels.fromAbgr(pixels);

        assertArrayEquals(
                new byte[] {
                    0x11,
                    0x22,
                    0x33,
                    0x44,
                    0x7F,
                    (byte) 0xFF,
                    0x00,
                    (byte) 0x80,
                },
                rgba);
    }
}
