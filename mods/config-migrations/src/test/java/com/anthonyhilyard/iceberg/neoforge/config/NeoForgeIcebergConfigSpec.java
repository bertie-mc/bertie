package com.anthonyhilyard.iceberg.neoforge.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import java.util.List;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;

/** Minimal test double for Iceberg's custom NeoForge config specification. */
public final class NeoForgeIcebergConfigSpec implements IConfigSpec {
    private boolean enabled;
    private int count;

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void validateSpec(ModConfig config) {
    }

    @Override
    public boolean isCorrect(UnmodifiableCommentedConfig config) {
        Object settings = config.getRaw(List.of("settings"));
        if (!(settings instanceof UnmodifiableCommentedConfig table)) {
            return false;
        }
        Object enabled = table.getRaw(List.of("enabled"));
        Object count = table.getRaw(List.of("count"));
        return enabled instanceof Boolean
                && count instanceof Number number
                && number.intValue() >= 1
                && number.intValue() <= 10;
    }

    @Override
    public void correct(CommentedConfig config) {
        Object existing = config.getRaw(List.of("settings"));
        CommentedConfig settings;
        if (existing instanceof CommentedConfig table) {
            settings = table;
        } else {
            settings = config.createSubConfig();
            config.set(List.of("settings"), settings);
        }

        if (!(settings.getRaw(List.of("enabled")) instanceof Boolean)) {
            settings.set(List.of("enabled"), true);
        }
        Object count = settings.getRaw(List.of("count"));
        if (!(count instanceof Number number)
                || number.intValue() < 1
                || number.intValue() > 10) {
            settings.set(List.of("count"), 3);
        }
    }

    @Override
    public void acceptConfig(ILoadedConfig config) {
        if (config != null) {
            enabled = config.config().get("settings.enabled");
            count = config.config().<Number>get("settings.count").intValue();
        }
    }

    public boolean enabled() {
        return enabled;
    }

    public int count() {
        return count;
    }
}
