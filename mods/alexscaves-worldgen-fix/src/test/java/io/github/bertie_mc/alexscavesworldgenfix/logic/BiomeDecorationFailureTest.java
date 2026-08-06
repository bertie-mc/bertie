package io.github.bertie_mc.alexscavesworldgenfix.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BiomeDecorationFailureTest {

    /** The runtime name observed in the crash log - Mixin's rename of the handler. */
    private static final String RUNTIME_HANDLER_NAME = "redirect$dfj000$alexscaves$ac_clampBiomeDecorationIndex";

    /** Alex's Caves 2.0.10, transcribed from the shipped bytecode. */
    private static Object alexsCavesClamp(List<?> list, int index) {
        if (index < 0 || index >= list.size()) {
            int safeIndex = Math.max(0, Math.min(index, list.size() - 1));
            return list.get(safeIndex);
        }
        return list.get(index);
    }

    @Test
    void upstreamClampThrowsOnAnEmptyList() {
        // max(0, min(-1, -1)) == 0, so the clamp still calls get(0) on an empty list.
        IndexOutOfBoundsException thrown =
                assertThrows(IndexOutOfBoundsException.class, () -> alexsCavesClamp(List.of(), -1));

        assertEquals("Index 0 out of bounds for length 0", thrown.getMessage());
    }

    @Test
    void upstreamClampStillWorksWhenTheListHasEntries() {
        List<String> features = List.of("first", "second");

        assertEquals("first", alexsCavesClamp(features, -1));
        assertEquals("second", alexsCavesClamp(features, 1));
        assertEquals("second", alexsCavesClamp(features, 7));
    }

    @Test
    void recognisesTheClampFailure() {
        assertTrue(BiomeDecorationFailure.isAlexsCavesClamp(clampFailure()));
    }

    @Test
    void recognisesTheClampFailureThroughVanillasReportedExceptionWrapper() {
        // Vanilla catches the decoration loop and rethrows a ReportedException, so the
        // IndexOutOfBoundsException is never the outermost throwable in practice.
        Throwable wrapped = new RuntimeException("Biome decoration", clampFailure());

        assertTrue(BiomeDecorationFailure.isAlexsCavesClamp(wrapped));
    }

    @Test
    void rejectsAnIndexFailureFromSomewhereElse() {
        // Same exception type, different origin: another mod's worldgen bug must keep crashing.
        IndexOutOfBoundsException elsewhere = new IndexOutOfBoundsException("Index 0 out of bounds for length 0");
        elsewhere.setStackTrace(new StackTraceElement[] {
            frame("net.minecraft.world.level.levelgen.placement.PlacedFeature", "placeWithBiomeCheck"),
        });

        assertFalse(BiomeDecorationFailure.isAlexsCavesClamp(elsewhere));
    }

    @Test
    void rejectsADifferentExceptionTypeFromTheClamp() {
        RuntimeException wrongType = new RuntimeException("something else entirely");
        wrongType.setStackTrace(new StackTraceElement[] {
            frame("net.minecraft.world.level.chunk.ChunkGenerator", RUNTIME_HANDLER_NAME),
        });

        assertFalse(BiomeDecorationFailure.isAlexsCavesClamp(wrongType));
    }

    @Test
    void toleratesASelfReferentialCauseChain() {
        // A cycle here would hang the worldgen thread instead of crashing it - no improvement.
        Throwable looping = new RuntimeException("loops") {
            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        };

        assertFalse(BiomeDecorationFailure.isAlexsCavesClamp(looping));
    }

    @Test
    void reportsTheFirstFewFailuresThenEveryHundredth() {
        assertFalse(BiomeDecorationFailure.shouldReport(0L));
        assertTrue(BiomeDecorationFailure.shouldReport(1L));
        assertTrue(BiomeDecorationFailure.shouldReport(3L));
        assertFalse(BiomeDecorationFailure.shouldReport(4L));
        assertFalse(BiomeDecorationFailure.shouldReport(99L));
        assertTrue(BiomeDecorationFailure.shouldReport(100L));
        assertFalse(BiomeDecorationFailure.shouldReport(101L));
        assertTrue(BiomeDecorationFailure.shouldReport(1_200L));
    }

    /** The real failure: the upstream clamp thrown from a frame Mixin has renamed. */
    private static IndexOutOfBoundsException clampFailure() {
        IndexOutOfBoundsException thrown =
                assertThrows(IndexOutOfBoundsException.class, () -> alexsCavesClamp(new ArrayList<>(), -1));
        thrown.setStackTrace(new StackTraceElement[] {
            frame("java.util.ArrayList", "get"),
            frame("net.minecraft.world.level.chunk.ChunkGenerator", RUNTIME_HANDLER_NAME),
            frame("net.minecraft.world.level.chunk.ChunkGenerator", "applyBiomeDecoration"),
        });
        return thrown;
    }

    private static StackTraceElement frame(String declaringClass, String method) {
        return new StackTraceElement(declaringClass, method, null, -1);
    }
}
