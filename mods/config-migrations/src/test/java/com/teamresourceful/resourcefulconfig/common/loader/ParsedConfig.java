package com.teamresourceful.resourcefulconfig.common.loader;

import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig;
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigEntry;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

/** Concrete JSONC-backed config fixture matching the integration's supported type. */
public final class ParsedConfig implements ResourcefulConfig {
    private final String id;
    private final Path file;
    private final LinkedHashMap<String, ResourcefulConfigEntry> entries = new LinkedHashMap<>();
    private final LinkedHashMap<String, ResourcefulConfig> categories = new LinkedHashMap<>();
    private int saves;

    public ParsedConfig(String id, Path file) {
        this.id = id;
        this.file = file;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public LinkedHashMap<String, ResourcefulConfigEntry> entries() {
        return entries;
    }

    @Override
    public LinkedHashMap<String, ResourcefulConfig> categories() {
        return categories;
    }

    @Override
    public void save() {
        saves++;
        try {
            String fileName = file.getFileName().toString();
            Path legacy = file.resolveSibling(fileName.substring(0, fileName.length() - 1));
            Path destination = Files.exists(legacy) ? legacy : file;
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, Writer.save(this).toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // Resourceful Config 3.0.11 logs and returns from save failures.
        }
    }

    public int saves() {
        return saves;
    }
}
