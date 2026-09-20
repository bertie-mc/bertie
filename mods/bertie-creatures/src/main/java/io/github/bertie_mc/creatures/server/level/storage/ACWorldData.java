package io.github.bertie_mc.creatures.server.level.storage;

import io.github.bertie_mc.creatures.server.entity.living.LuxtructosaurusEntity;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class ACWorldData extends SavedData {
    private final Map<UUID, Integer> reputations = new HashMap<>();
    private final Set<Integer> bosses = new HashSet<>();

    public static ACWorldData get(Level level) {
        return level instanceof ServerLevel server
                ? server.getDataStorage()
                        .computeIfAbsent(new Factory<>(ACWorldData::new, ACWorldData::load), "bertiecreatures")
                : null;
    }

    private static ACWorldData load(CompoundTag tag, HolderLookup.Provider provider) {
        var result = new ACWorldData();
        var entries = tag.getList("DeepOneReputations", 10);
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.getCompound(i);
            result.reputations.put(entry.getUUID("UUID"), entry.getInt("Reputation"));
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        var entries = new ListTag();
        reputations.forEach((id, rep) -> {
            var entry = new CompoundTag();
            entry.putUUID("UUID", id);
            entry.putInt("Reputation", rep);
            entries.add(entry);
        });
        tag.put("DeepOneReputations", entries);
        return tag;
    }

    public int getDeepOneReputation(UUID id) {
        return reputations.getOrDefault(id, 0);
    }

    public void setDeepOneReputation(UUID id, int reputation) {
        reputations.put(id, Mth.clamp(reputation, -100, 100));
        setDirty();
    }

    public void trackPrimordialBoss(int id, boolean active) {
        if (active) bosses.add(id);
        else bosses.remove(id);
    }

    public boolean isPrimordialBossActive(Level level) {
        return bosses.stream()
                .anyMatch(id -> level.getEntity(id) instanceof LuxtructosaurusEntity boss && boss.isAlive());
    }
}
