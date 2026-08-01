package com.berlord.hephaestusarchitecture.structure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureResourcesTest {

    @Test
    void shippedLayoutsContainOneAnchorAndTheirExpectedPedestals() throws IOException {
        assertLayout(2, 8);
        assertLayout(3, 12);
        assertLayout(4, 12);
    }

    @Test
    void ponderAndDatapackTemplatesStayIdentical() throws IOException {
        for (int tier = 2; tier <= 4; tier++) {
            assertArrayEquals(bytes("/data/hephaestusarchitecture/structure/hephaestus_forge/tier_"
                            + tier + ".nbt"),
                    bytes("/assets/hephaestusarchitecture/ponder/tier_" + tier + ".nbt"),
                    "tier " + tier);
        }
    }

    private static void assertLayout(int tier, int expectedPedestals) throws IOException {
        try (InputStream input = resource(
                "/data/hephaestusarchitecture/structure/hephaestus_forge/tier_" + tier + ".nbt")) {
            CompoundTag root = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
            ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
            assertEquals(3, root.getList("size", Tag.TAG_INT).size(), "tier " + tier + " size");
            assertTrue(!palette.isEmpty() && !blocks.isEmpty(), "tier " + tier + " must not be empty");

            Set<Integer> anchorStates = statesNamed(palette,
                    "forbidden_arcanus:hephaestus_forge_tier_" + tier);
            Set<Integer> pedestalStates = new HashSet<>();
            for (int index = 0; index < palette.size(); index++) {
                if (palette.getCompound(index).getString("Name").endsWith("_pedestal")) {
                    pedestalStates.add(index);
                }
            }

            assertEquals(1, countBlocks(blocks, anchorStates), "tier " + tier + " forge anchors");
            assertEquals(expectedPedestals, countBlocks(blocks, pedestalStates),
                    "tier " + tier + " pedestals");
        }
    }

    private static Set<Integer> statesNamed(ListTag palette, String name) {
        Set<Integer> matches = new HashSet<>();
        for (int index = 0; index < palette.size(); index++) {
            if (name.equals(palette.getCompound(index).getString("Name"))) {
                matches.add(index);
            }
        }
        return matches;
    }

    private static long countBlocks(ListTag blocks, Set<Integer> states) {
        return blocks.stream()
                .map(CompoundTag.class::cast)
                .filter(block -> states.contains(block.getInt("state")))
                .count();
    }

    private static InputStream resource(String path) {
        InputStream input = StructureResourcesTest.class.getResourceAsStream(path);
        assertNotNull(input, path);
        return input;
    }

    private static byte[] bytes(String path) throws IOException {
        try (InputStream input = resource(path)) {
            return input.readAllBytes();
        }
    }
}
