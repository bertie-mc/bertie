package io.github.bertie_mc.pack;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.jar.JarFile;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

final class PackArtifactClassificationTest {
    private static final String INVENTORY_PROPERTY = "bertie.pack.modArtifactInventory";

    @Test
    void packOnlyModsRequireFakePackMetadata() throws IOException {
        var inventory = Path.of(System.getProperty(INVENTORY_PROPERTY));
        var disguisedPacks = new ArrayList<String>();

        for (String line : Files.readAllLines(inventory)) {
            var fields = line.split("\\t", 3);
            if (fields.length != 3 || !(fields[1].equals("true") || fields[1].equals("false"))) {
                fail("Malformed external mod inventory line: " + line);
            }
            inspect(fields[0], Boolean.parseBoolean(fields[1]), Path.of(fields[2]), disguisedPacks);
        }

        if (!disguisedPacks.isEmpty()) {
            fail("""
                    These [mods.*] artifacts are standalone packs with no executable code:
                    %s
                    Declare portable pack distributions under [datapacks.*] or [resourcepacks.*].
                    If provider compatibility requires installation through mods/, keep the
                    artifact under [mods.*] and mark it with fakePack = true.
                    """.formatted(String.join("\n", disguisedPacks)));
        }
    }

    private static void inspect(String id, boolean fakePack, Path archive, ArrayList<String> disguisedPacks)
            throws IOException {
        try (var jar = new JarFile(archive.toFile())) {
            var entries = jar.stream().map(entry -> entry.getName()).toList();
            boolean hasPackMetadata = entries.contains("pack.mcmeta");
            boolean hasDatapackContent =
                    entries.stream().anyMatch(name -> name.startsWith("data/") && !name.endsWith("/"));
            boolean hasResourcepackContent =
                    entries.stream().anyMatch(name -> name.startsWith("assets/") && !name.endsWith("/"));
            boolean hasExecutableCode = entries.stream().anyMatch(name -> name.endsWith(".class"));
            boolean hasNestedExecutableCode = containsNestedExecutableCode(jar);

            if (hasPackMetadata
                    && (hasDatapackContent || hasResourcepackContent)
                    && !hasExecutableCode
                    && !hasNestedExecutableCode
                    && !fakePack) {
                var kinds = new ArrayList<String>();
                if (hasDatapackContent) {
                    kinds.add("datapack");
                }
                if (hasResourcepackContent) {
                    kinds.add("resourcepack");
                }
                disguisedPacks.add("- " + id + " (" + String.join(" + ", kinds) + ")");
            }
        }
    }

    private static boolean containsNestedExecutableCode(JarFile jar) throws IOException {
        for (var entry : jar.stream()
                .filter(candidate -> candidate.getName().endsWith(".jar"))
                .toList()) {
            try (var nested = new ZipInputStream(jar.getInputStream(entry))) {
                for (var nestedEntry = nested.getNextEntry();
                        nestedEntry != null;
                        nestedEntry = nested.getNextEntry()) {
                    if (!nestedEntry.isDirectory() && nestedEntry.getName().endsWith(".class")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
