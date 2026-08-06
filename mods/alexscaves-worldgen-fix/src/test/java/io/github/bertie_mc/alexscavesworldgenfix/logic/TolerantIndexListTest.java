package io.github.bertie_mc.alexscavesworldgenfix.logic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TolerantIndexListTest {

    private static final String FALLBACK = "no-op";

    /** Alex's Caves 2.0.10, transcribed from the shipped bytecode. */
    private static Object alexsCavesClamp(List<?> list, int index) {
        if (index < 0 || index >= list.size()) {
            int safeIndex = Math.max(0, Math.min(index, list.size() - 1));
            return list.get(safeIndex);
        }
        return list.get(index);
    }

    @Test
    void theEmptyStepNoLongerThrowsThroughUpstreamsClamp() {
        // STRONGHOLDS is empty and the index mapping misses, so the clamp reaches get(0) on
        // nothing. That is the crash that used to cost the chunk every step after this one.
        List<String> wrapped = TolerantIndexList.wrap(List.of(), FALLBACK, () -> {});

        assertEquals(FALLBACK, assertDoesNotThrow(() -> alexsCavesClamp(wrapped, -1)));
    }

    @Test
    void aPopulatedStepIsUntouched() {
        List<String> wrapped = TolerantIndexList.wrap(List.of("first", "second"), FALLBACK, () -> {});

        assertEquals("first", wrapped.get(0));
        assertEquals("second", wrapped.get(1));
        assertEquals(2, wrapped.size());
    }

    @Test
    void upstreamsChoiceOnAPopulatedListIsNotChanged() {
        // size() still reports the truth, so the clamp picks exactly what it picked before. Only
        // the empty case behaves differently; this mod does not get to redirect worldgen.
        List<String> raw = List.of("first", "second");
        List<String> wrapped = TolerantIndexList.wrap(raw, FALLBACK, () -> {});

        assertEquals(alexsCavesClamp(raw, 1), alexsCavesClamp(wrapped, 1));
        assertEquals("first", alexsCavesClamp(wrapped, -1));
        assertEquals("second", alexsCavesClamp(wrapped, 7));
    }

    @Test
    void reportsOnlyTheIndicesItHadToSubstitute() {
        AtomicInteger substitutions = new AtomicInteger();
        List<String> wrapped = TolerantIndexList.wrap(List.of("first"), FALLBACK, substitutions::incrementAndGet);

        wrapped.get(0);
        assertEquals(0, substitutions.get());

        wrapped.get(-1);
        wrapped.get(9);
        assertEquals(2, substitutions.get());
    }

    @Test
    void negativeIndicesFallBackRatherThanThrow() {
        // Without Alex's Caves in front of it, vanilla hands the raw -1 straight to get.
        List<String> wrapped = TolerantIndexList.wrap(List.of("first"), FALLBACK, () -> {});

        assertEquals(FALLBACK, assertDoesNotThrow(() -> wrapped.get(-1)));
    }
}
