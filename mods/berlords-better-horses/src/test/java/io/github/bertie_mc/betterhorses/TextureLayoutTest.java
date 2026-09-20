package io.github.bertie_mc.betterhorses;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class TextureLayoutTest {
    @Test
    void hoofBackGapRemainsUnpainted() throws IOException {
        for (String material : new String[] {"iron", "gold", "diamond", "netherite"}) {
            try (var input = getClass()
                    .getResourceAsStream("/assets/betterhorses/textures/entity/horseshoes_" + material + ".png")) {
                assertNotNull(input);
                var image = ImageIO.read(input);
                assertEquals(64, image.getWidth());
                assertEquals(64, image.getHeight());
                assertEquals(0, image.getRGB(61, 35) >>> 24);
                assertEquals(0, image.getRGB(62, 35) >>> 24);
                for (int x : new int[] {48, 52, 56, 60, 63}) assertEquals(255, image.getRGB(x, 35) >>> 24);
                for (int x = 48; x < 64; x++) assertEquals(0, image.getRGB(x, 34) >>> 24);
            }
        }
    }

    @Test
    void inventoryTexturesAreNative16Pixels() throws IOException {
        for (String item : new String[] {
            "horse_effigy",
            "zombie_apple",
            "skeleton_apple",
            "breed_apple",
            "iron_horseshoes",
            "gold_horseshoes",
            "diamond_horseshoes",
            "netherite_horseshoes",
            "netherite_horse_armor",
            "passenger_saddle",
            "warrior_saddle",
            "wanderer_saddle"
        }) {
            try (var input = getClass().getResourceAsStream("/assets/betterhorses/textures/item/" + item + ".png")) {
                assertNotNull(input);
                var image = ImageIO.read(input);
                assertEquals(16, image.getWidth());
                assertEquals(16, image.getHeight());
                assertTrue(image.getColorModel().hasAlpha());
            }
        }
    }
}
