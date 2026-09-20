package io.github.bertie_mc.armorcompletions;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class ApprovedTexturesTest {
    @Test void everyGameTextureMatchesTheApprovedInstalledVersion() throws Exception {
        try (var manifest = getClass().getResourceAsStream("/approved-texture-sha256.json")) {
            var hashes = JsonParser.parseReader(new InputStreamReader(manifest, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(17, hashes.size());
            for (var entry : hashes.entrySet()) {
                try (var texture = getClass().getResourceAsStream("/" + entry.getKey())) {
                    assertNotNull(texture, entry.getKey());
                    String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texture.readAllBytes()));
                    assertEquals(entry.getValue().getAsString(), actual, "Approved texture changed: " + entry.getKey());
                }
            }
        }
    }
}
