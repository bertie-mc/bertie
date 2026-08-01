package com.berlord.forgeink;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeInkResourcesTest {

    private static final Pattern MIXIN_NAME = Pattern.compile("\\\"([A-Za-z]+Mixin)\\\"");

    @Test
    void declaresEveryRequiredMixin() throws IOException {
        String config = resourceText("/forgeink.mixins.json");
        Matcher matcher = MIXIN_NAME.matcher(config);
        Set<String> declared = new java.util.HashSet<>();
        while (matcher.find()) {
            declared.add(matcher.group(1));
        }

        assertTrue(config.contains("\"required\": true"));
        assertEquals(Set.of(
                "EssenceDataInputMixin",
                "EssenceStorageInputMixin",
                "ExtractEnchantmentsInputMixin",
                "HephaestusForgeBlockEntityMixin",
                "HephaestusForgeMenuMixin"
        ), declared);
    }

    @Test
    void shipsTheForcedUiOverrideResources() throws IOException {
        String root = "/resourcepacks/forgeink_overrides/";
        assertTrue(resourceText(root + "pack.mcmeta").contains("\"pack_format\": 34"));
        assertTrue(resourceText(root + "assets/forbidden_arcanus/lang/en_us.json")
                .contains("\"essence.forbidden_arcanus.experience\": \"Ink\""));
        assertNotNull(getClass().getResource(root
                + "assets/forbidden_arcanus/textures/gui/container/hephaestus_forge.png"));
    }

    private String resourceText(String path) throws IOException {
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing test resource: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
