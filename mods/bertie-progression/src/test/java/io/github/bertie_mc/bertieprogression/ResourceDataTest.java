package io.github.bertie_mc.bertieprogression;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResourceDataTest {

    private static final Path RESOURCES = resources();

    private static Path resources() {
        try {
            return Path.of(ResourceDataTest.class
                            .getResource("/assets/bertieprogression")
                            .toURI())
                    .getParent()
                    .getParent();
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void everyJsonResourceParses() throws IOException {
        List<String> failures = new ArrayList<>();
        long count;
        try (var files = Files.walk(RESOURCES)) {
            List<Path> jsonFiles =
                    files.filter(path -> path.toString().endsWith(".json")).toList();
            count = jsonFiles.size();
            for (Path path : jsonFiles) {
                try {
                    JsonParser.parseString(Files.readString(path));
                } catch (RuntimeException failure) {
                    failures.add(RESOURCES.relativize(path) + ": " + failure.getMessage());
                }
            }
        }
        assertTrue(count >= 700, "expected the progression data set, found " + count + " JSON files");
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    @Test
    void localItemTextureReferencesExist() throws IOException {
        Path models = RESOURCES.resolve("assets/bertieprogression/models/item");
        List<String> missing = new ArrayList<>();
        try (var files = Files.walk(models)) {
            for (Path model :
                    files.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonElement parsed = JsonParser.parseString(Files.readString(model));
                if (!parsed.getAsJsonObject().has("textures")) {
                    continue;
                }
                for (JsonElement texture : parsed.getAsJsonObject()
                        .getAsJsonObject("textures")
                        .asMap()
                        .values()) {
                    String id = texture.getAsString();
                    if (!id.startsWith("bertieprogression:")) {
                        continue;
                    }
                    Path image = RESOURCES
                            .resolve("assets/bertieprogression/textures/")
                            .resolve(id.substring("bertieprogression:".length()) + ".png");
                    if (!Files.isRegularFile(image)) {
                        missing.add(RESOURCES.relativize(model) + " -> " + RESOURCES.relativize(image));
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), String.join("\n", missing));
    }

    /**
     * Every Hephaestus Forge tier upgrade must start from the tier below the one it grants, so the
     * chain from Tier 1 has no gap and no branch. Forbidden &amp; Arcanus defaults a missing
     * {@code forge_tier} to 1, which is silent and correct only for the very first upgrade; an
     * override that drops the field from a later one strands the player on whichever tier they
     * reached (upgrade_tier_3 did exactly that).
     */
    @Test
    void forgeTierUpgradesFormAContinuousChain() throws IOException {
        Path rituals = RESOURCES.resolve("data/forbidden_arcanus/forbidden_arcanus/hephaestus_forge/ritual");
        List<String> broken = new ArrayList<>();
        try (var files = Files.walk(rituals)) {
            for (Path ritual :
                    files.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonObject parsed =
                        JsonParser.parseString(Files.readString(ritual)).getAsJsonObject();
                JsonObject result = parsed.getAsJsonObject("result");
                if (result == null
                        || !"forbidden_arcanus:upgrade_tier"
                                .equals(result.get("type").getAsString())) {
                    continue;
                }
                int granted = result.get("result_tier").getAsInt();
                int required =
                        parsed.has("forge_tier") ? parsed.get("forge_tier").getAsInt() : 1;
                if (required != granted - 1) {
                    broken.add(RESOURCES.relativize(ritual) + ": runs on Tier " + required + " but grants Tier "
                            + granted);
                }
            }
        }
        assertTrue(broken.isEmpty(), String.join("\n", broken));
    }
}
