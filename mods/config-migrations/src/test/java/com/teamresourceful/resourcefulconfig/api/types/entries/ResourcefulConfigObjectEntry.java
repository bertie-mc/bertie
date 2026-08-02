package com.teamresourceful.resourcefulconfig.api.types.entries;

import java.util.LinkedHashMap;

public interface ResourcefulConfigObjectEntry extends ResourcefulConfigEntry {
    LinkedHashMap<String, ResourcefulConfigEntry> entries();
}
