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
    public net.minecraft.world.phys.Vec3 originalDirection = net.minecraft.world.phys.Vec3.ZERO, rangePosition;
    public double remainingRange = -1;
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
        originalDirection = new net.minecraft.world.phys.Vec3(
                saved.getDouble("aimX"), saved.getDouble("aimY"), saved.getDouble("aimZ"));
        remainingRange = saved.contains("range") ? saved.getDouble("range") : -1;
        if (saved.contains("rangeX"))
            rangePosition = new net.minecraft.world.phys.Vec3(
                    saved.getDouble("rangeX"), saved.getDouble("rangeY"), saved.getDouble("rangeZ"));
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
        saved.putDouble("aimX", originalDirection.x);
        saved.putDouble("aimY", originalDirection.y);
        saved.putDouble("aimZ", originalDirection.z);
        saved.putDouble("range", remainingRange);
        if (rangePosition != null) {
            saved.putDouble("rangeX", rangePosition.x);
            saved.putDouble("rangeY", rangePosition.y);
            saved.putDouble("rangeZ", rangePosition.z);
        }
        ListTag hits = new ListTag();
        hit.forEach(id -> hits.add(StringTag.valueOf(id.toString())));
        saved.put("hit", hits);
        ListTag area = new ListTag();
        areaHit.forEach(id -> area.add(StringTag.valueOf(id.toString())));
        saved.put("area", area);
        tag.put("bertieFlight", saved);
    }

    public void beginSecondary(net.minecraft.world.phys.Vec3 position) {
        if (remainingRange < 0) {
            remainingRange = 24;
            rangePosition = position;
        }
    }

    public void travelledTo(net.minecraft.world.phys.Vec3 position) {
        if (remainingRange >= 0 && rangePosition != null) {
            remainingRange = Math.max(0, remainingRange - rangePosition.distanceTo(position));
            rangePosition = position;
        }
    }
}
