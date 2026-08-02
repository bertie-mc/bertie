package de.ambertation.wunderlib.configs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import de.ambertation.wunderlib.utils.Version;
import net.minecraft.resources.ResourceLocation;

public abstract class AbstractConfig<C extends AbstractConfig<C>> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public final ResourceLocation location;
    private JsonObject root;
    private int saveCount;

    protected AbstractConfig(
            Version.ModVersionProvider versionProvider,
            String namespace,
            String category) {
        location = versionProvider.mk(category);
    }

    protected abstract JsonObject loadRootElement();

    protected abstract boolean saveRootElement(String contents);

    protected abstract boolean isReadOnly();

    public void loadFromDisc() {
        root = loadRootElement();
        if (root == null) {
            root = new JsonObject();
        }
    }

    public void save(boolean force) {
        if (!isReadOnly() && saveRootElement(GSON.toJson(root))) {
            saveCount++;
        }
    }

    public JsonObject fixtureRoot() {
        return root;
    }

    public int fixtureSaveCount() {
        return saveCount;
    }
}
