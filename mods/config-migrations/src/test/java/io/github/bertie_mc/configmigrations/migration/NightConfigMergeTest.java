package io.github.bertie_mc.configmigrations.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest.Change;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NightConfigMergeTest {
    @Test
    void recursivelyMergesTablesAndReplacesLeaves() {
        CommentedConfig target = TomlFormat.newConfig();
        target.set(List.of("settings", "changed"), true);
        target.set(List.of("settings", "kept"), "player value");
        target.set(List.of("replaceTable"), 1);
        target.set(List.of("values"), List.of(1, 2));

        CommentedConfig fragment = TomlFormat.newConfig();
        fragment.set(List.of("settings", "changed"), false);
        fragment.set(List.of("replaceTable", "child"), 2);
        fragment.set(List.of("values"), List.of(3, 4));
        fragment.set(List.of("literal.dot"), 5);

        NightConfigMerge.apply(target, List.of(new Change(1, fragment)));

        assertFalse(target.<Boolean>get(List.of("settings", "changed")));
        assertEquals("player value", target.get(List.of("settings", "kept")));
        assertEquals(2, target.<Number>get(List.of("replaceTable", "child")).intValue());
        assertEquals(List.of(3, 4), target.get(List.of("values")));
        assertEquals(5, target.<Number>get(List.of("literal.dot")).intValue());
    }

    @Test
    void mutableFragmentContainersAreNotSharedWithLiveConfigs() {
        CommentedConfig fragment = TomlFormat.newConfig();
        List<Integer> values = new ArrayList<>(List.of(1, 2));
        CommentedConfig item = fragment.createSubConfig();
        item.set("name", "original");
        fragment.set("values", values);
        fragment.set("items", new ArrayList<>(List.of(item)));
        Change change = new Change(1, fragment);

        CommentedConfig first = TomlFormat.newConfig();
        NightConfigMerge.apply(first, List.of(change));
        first.<List<Integer>>get("values").add(3);
        first.<List<CommentedConfig>>get("items").getFirst().set("name", "changed");

        assertEquals(List.of(1, 2), values);
        assertEquals("original", item.get("name"));

        CommentedConfig second = TomlFormat.newConfig();
        NightConfigMerge.apply(second, List.of(change));
        assertEquals(List.of(1, 2), second.get("values"));
        assertEquals(
                "original",
                second.<List<CommentedConfig>>get("items").getFirst().get("name"));
    }
}
