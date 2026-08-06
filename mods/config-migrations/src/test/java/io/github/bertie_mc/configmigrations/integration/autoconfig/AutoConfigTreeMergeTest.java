package io.github.bertie_mc.configmigrations.integration.autoconfig;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest.Change;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class AutoConfigTreeMergeTest {
    @Test
    void mergesTheLogicalObjectTreeWithoutReplacingUnmentionedValues() {
        SampleConfig target = new SampleConfig();
        CommentedConfig fragment = TomlFormat.newConfig();
        fragment.set("nested.enabled", false);
        fragment.set("nested.limit", 9L);
        fragment.set("names", List.of("changed", "again"));
        fragment.set("thresholds", List.of(2L, 4L, 8L));
        fragment.set("relations.friend", "ALLY");

        AutoConfigTreeMerge.apply(target, List.of(new Change(1, fragment)));

        assertFalse(target.nested.enabled);
        assertEquals(9, target.nested.limit);
        assertEquals("preserved", target.nested.untouched);
        assertEquals(List.of("changed", "again"), target.names);
        assertArrayEquals(new int[] {2, 4, 8}, target.thresholds);
        assertEquals(Relation.ALLY, target.relations.get("friend"));
        assertEquals(Relation.HOSTILE, target.relations.get("enemy"));
    }

    @Test
    void rejectsAFieldThatIsNotPartOfTheConfigDataModel() {
        CommentedConfig fragment = TomlFormat.newConfig();
        fragment.set("missing", true);

        assertThrows(
                ConfigMigrationException.class,
                () -> AutoConfigTreeMerge.apply(new SampleConfig(), List.of(new Change(1, fragment))));
    }

    private enum Relation {
        ALLY,
        HOSTILE
    }

    private static final class SampleConfig {
        private final Nested nested = new Nested();
        private List<String> names = new ArrayList<>(List.of("original"));
        private int[] thresholds = {1};
        private LinkedHashMap<String, Relation> relations = new LinkedHashMap<>();

        private SampleConfig() {
            relations.put("enemy", Relation.HOSTILE);
        }
    }

    private static final class Nested {
        private boolean enabled = true;
        private int limit = 3;
        private String untouched = "preserved";
    }
}
