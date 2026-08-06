package artifacts.config;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Minimal stand-in for Artifacts' config manager, including its migration hook anchor. */
public final class ConfigManager implements AutoCloseable {
    protected CommentedFileConfig config;
    protected final ConfigSpec spec = new ConfigSpec();

    private final String name;
    private final Path configPath;
    private final Path statePath;
    private boolean enabledWhenConsumed;
    private int countWhenConsumed;
    private String stateWhenConsumed;

    public ConfigManager(String name, Path configPath, Path statePath) {
        this.name = name;
        this.configPath = configPath;
        this.statePath = statePath;
        spec.define(List.of("settings", "enabled"), true);
        spec.defineInRange(List.of("settings", "count"), 3, 1, 10);
    }

    public String getName() {
        return name;
    }

    public void load() {
        setup();
    }

    protected void setup() {
        config = CommentedFileConfig.builder(configPath).sync().build();
        config.load();
        if (!spec.isCorrect(config)) {
            spec.correct(config);
            config.save();
        }

        FileWatcher.defaultInstance();
        enabledWhenConsumed = config.get(List.of("settings", "enabled"));
        countWhenConsumed = config.<Number>get(List.of("settings", "count")).intValue();
        try {
            stateWhenConsumed =
                    Files.exists(statePath) ? Files.readString(statePath).strip() : null;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public boolean enabledWhenConsumed() {
        return enabledWhenConsumed;
    }

    public int countWhenConsumed() {
        return countWhenConsumed;
    }

    public String stateWhenConsumed() {
        return stateWhenConsumed;
    }

    @Override
    public void close() {
        if (config != null) {
            config.close();
        }
    }
}
