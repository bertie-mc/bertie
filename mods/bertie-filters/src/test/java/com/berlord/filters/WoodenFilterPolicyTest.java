package com.berlord.filters;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WoodenFilterPolicyTest {

    @Test
    void handlesOnlyTheWoodenFilterId() {
        assertTrue(WoodenFilterPolicy.handles("bertie:wooden"));
        assertFalse(WoodenFilterPolicy.handles("bertie:metal"));
        assertFalse(WoodenFilterPolicy.handles("other:wooden"));
        assertFalse(WoodenFilterPolicy.handles(null));
    }

    @Test
    void matchesARequestedToolWithAtLeastOneWoodenPart() {
        assertTrue(WoodenFilterPolicy.matches(
                "pickaxe",
                "slag:pickaxe",
                List.of("slag:stone", "slag:wooden", "slag:iron")
        ));
    }

    @Test
    void rejectsWrongModularTypeOrNamespace() {
        assertFalse(WoodenFilterPolicy.matches("pickaxe", "slag:axe", List.of("slag:wooden")));
        assertFalse(WoodenFilterPolicy.matches("pickaxe", "other:pickaxe", List.of("slag:wooden")));
        assertFalse(WoodenFilterPolicy.matches(null, "slag:pickaxe", List.of("slag:wooden")));
    }

    @Test
    void rejectsItemsWithoutAWoodenSlagPart() {
        assertFalse(WoodenFilterPolicy.matches("helmet", "slag:helmet", List.of()));
        assertFalse(WoodenFilterPolicy.matches(
                "helmet",
                "slag:helmet",
                List.of("slag:iron", "other:wooden")
        ));
    }
}
