package io.github.bertie_mc.configmigrations.integration.supermartijn642;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.supermartijn642.configlib.toml.TomlConfigFile;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SuperMartijn642IntegrationTest {
    private static final String FILE = "sample-common.toml";

    @TempDir
    Path gameDirectory;

    @Test
    void nativeDocumentIsMergedAndStateCommitsAfterNativeSave() throws Exception {
        Path manifests = gameDirectory.resolve("migrations/supermartijn642");
        Path target = gameDirectory.resolve("config").resolve(FILE);
        Path state = gameDirectory.resolve(
                "config/config-migrations/state/config/sample-common.toml.version");
        Files.createDirectories(manifests);
        Files.createDirectories(target.getParent());
        Files.writeString(manifests.resolve("sample.toml"), manifest());
        Files.writeString(target, "[settings]\nenabled = true\ncount = 3\n");
        TomlConfigFile document = new TomlConfigFile(target.toFile());
        document.readFile();
        SuperMartijn642Integration integration = SuperMartijn642Integration.load(
                MigrationManager.load(gameDirectory),
                gameDirectory.resolve("config"),
                manifests);

        integration.apply("sample", FILE, document);

        assertFalse(document.getValue(new String[] {"settings", "enabled"}).getAsBoolean());
        assertEquals(8, document.getValue(new String[] {"settings", "count"}).getAsInteger());
        assertFalse(Files.exists(state));

        document.writeFile();
        integration.commit("sample", FILE);

        assertEquals("2\n", Files.readString(state));
    }

    private static String manifest() {
        return """
                mod = "sample"
                file = "sample-common.toml"

                [[changes]]
                version = 2
                op = "merge"
                [changes.fragment.settings]
                enabled = false
                count = 8
                """;
    }
}
