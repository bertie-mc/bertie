package io.github.bertie_mc.creatures;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Detects resource extraction gaps that a dedicated server cannot see. */
class ResourceGraphTest {
    private static final Path ROOT = Path.of("src/main/resources");

    @Test
    void modelAndParticleDependenciesExist() throws IOException {
        try (var files = Files.walk(ROOT.resolve("assets/bertiecreatures"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonElement json = JsonParser.parseString(Files.readString(file));
                walk(json, "", "", file);
            }
        }
    }

    private static void walk(JsonElement value, String key, String parent, Path source) {
        if (value.isJsonObject()) {
            value.getAsJsonObject().entrySet().forEach(e -> walk(e.getValue(), e.getKey(), key, source));
        } else if (value.isJsonArray()) {
            value.getAsJsonArray().forEach(v -> walk(v, key, parent, source));
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String id = value.getAsString();
            if (!id.startsWith("bertiecreatures:")) return;
            String path = id.substring("bertiecreatures:".length());
            Path target;
            if (List.of("parent", "model").contains(key))
                target = ROOT.resolve("assets/bertiecreatures/models/" + path + ".json");
            else if (parent.equals("textures") || key.equals("textures")) {
                String prefix = source.getParent().getFileName().toString().equals("particles") ? "particle/" : "";
                target = ROOT.resolve("assets/bertiecreatures/textures/" + prefix + path + ".png");
            } else return;
            assertTrue(Files.isRegularFile(target), () -> source + " references missing " + target);
        }
    }

    @Test
    void dataDoesNotRequireOriginalNamespace() throws IOException {
        try (var files = Files.walk(ROOT.resolve("data"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                String text = Files.readString(file);
                assertDoesNotThrow(() -> JsonParser.parseString(text), file.toString());
                assertFalse(text.contains("alexscaves:"), file.toString());
            }
        }
    }
}
