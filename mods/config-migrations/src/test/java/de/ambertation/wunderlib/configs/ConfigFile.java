package de.ambertation.wunderlib.configs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ambertation.wunderlib.utils.Version;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import net.neoforged.fml.loading.FMLPaths;

public class ConfigFile extends AbstractConfig<ConfigFile> {
    private final File path;

    public ConfigFile(Version.ModVersionProvider versionProvider, String category) {
        this(versionProvider, versionProvider.getNamespace(), category);
    }

    public ConfigFile(
            Version.ModVersionProvider versionProvider,
            String basePath,
            String category) {
        super(versionProvider, basePath, category);
        File directory = FMLPaths.CONFIGDIR.get().resolve(basePath).toFile();
        path = new File(directory, category + ".json");
        directory.mkdirs();
        loadFromDisc();
    }

    @Override
    protected JsonObject loadRootElement() {
        if (!path.exists()) {
            return null;
        }
        try (FileReader reader = new FileReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override
    protected boolean saveRootElement(String contents) {
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(contents);
            return true;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override
    protected boolean isReadOnly() {
        return false;
    }
}
