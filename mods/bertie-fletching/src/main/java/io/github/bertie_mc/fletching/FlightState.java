package io.github.bertie_mc.fletching;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

public final class FlightState {
    public final ArrowProfile profile;
    public net.minecraft.world.phys.Vec3 redirectStart;
    public final Set<UUID> hit = new HashSet<>();
    public final Set<UUID> areaHit = new HashSet<>();
    public boolean started, tracing;
    public int age, ricochets, bounces, drilled, depth;
    public double peak, fixedDamage = -1;

    public FlightState(CompoundTag tag) {
        profile = ArrowProfile.read(tag);
        CompoundTag saved = tag.getCompound("bertieFlight");
        started = saved.getBoolean("started");
        age = saved.getInt("age");
        ricochets = saved.getInt("ricochets");
        bounces = saved.contains("bounces") ? saved.getInt("bounces") : 3;
        drilled = saved.getInt("drilled");
        depth = tag.getInt("bertieChildDepth");
        peak = saved.contains("peak") ? saved.getDouble("peak") : -Double.MAX_VALUE;
        if (tag.contains("bertieFixedDamage")) fixedDamage = tag.getDouble("bertieFixedDamage");
        readIds(saved, "hit", hit);
        readIds(saved, "area", areaHit);
    }

    private static void readIds(CompoundTag tag, String key, Set<UUID> into) {
        for (var entry : tag.getList(key, 8)) {
            try {
                into.add(UUID.fromString(entry.getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save(CompoundTag tag) {
        CompoundTag saved = new CompoundTag();
        saved.putBoolean("started", started);
        saved.putInt("age", age);
        saved.putInt("ricochets", ricochets);
        saved.putInt("bounces", bounces);
        saved.putInt("drilled", drilled);
        saved.putDouble("peak", peak);
        ListTag hits = new ListTag();
        hit.forEach(id -> hits.add(StringTag.valueOf(id.toString())));
        saved.put("hit", hits);
        ListTag area = new ListTag();
        areaHit.forEach(id -> area.add(StringTag.valueOf(id.toString())));
        saved.put("area", area);
        tag.put("bertieFlight", saved);
    }
}
