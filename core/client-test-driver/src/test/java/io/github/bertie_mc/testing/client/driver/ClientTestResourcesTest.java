package io.github.bertie_mc.testing.client.driver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ClientTestResourcesTest {
    @Test
    void closesResourcesInReverseCreationOrder() {
        List<String> closed = new ArrayList<>();
        ClientTestResources resources = new ClientTestResources();
        resources.own(() -> closed.add("first"));
        resources.own(() -> closed.add("second"));

        resources.close();
        resources.close();

        assertEquals(List.of("second", "first"), closed);
    }

    @Test
    void closesEveryResourceAndPreservesCleanupFailures() {
        List<String> closed = new ArrayList<>();
        ClientTestResources resources = new ClientTestResources();
        resources.own(() -> {
            closed.add("first");
            throw new IllegalStateException("first failure");
        });
        resources.own(() -> {
            closed.add("second");
            throw new IllegalArgumentException("second failure");
        });

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, resources::close);

        assertEquals(List.of("second", "first"), closed);
        assertEquals("second failure", failure.getMessage());
        assertEquals("first failure", failure.getSuppressed()[0].getMessage());
    }

    @Test
    void immediatelyClosesResourcesRegisteredAfterTeardown() {
        List<String> closed = new ArrayList<>();
        ClientTestResources resources = new ClientTestResources();
        resources.close();

        assertThrows(IllegalStateException.class, () -> resources.own(() -> closed.add("late")));
        assertEquals(List.of("late"), closed);
    }
}
