package io.github.bertie_mc.fletching;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class TankStorage extends SavedData {
    private final Map<BlockPos, PotionTank> tanks = new HashMap<>();

    public static TankStorage get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(new Factory<>(TankStorage::new, TankStorage::load, null), "bertie_fletching_tanks");
    }

    public PotionTank at(BlockPos pos) {
        return tanks.computeIfAbsent(pos.immutable(), key -> new PotionTank());
    }

    public void remove(BlockPos pos) {
        tanks.remove(pos);
        setDirty();
    }

    private static TankStorage load(CompoundTag tag, HolderLookup.Provider registries) {
        TankStorage storage = new TankStorage();
        for (var entry : tag.getList("Tanks", 10)) {
            CompoundTag tankTag = (CompoundTag) entry;
            storage.tanks.put(BlockPos.of(tankTag.getLong("Pos")), PotionTank.load(tankTag, registries));
        }
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        tanks.forEach((pos, tank) -> {
            if (tank.batches() > 0) {
                CompoundTag entry = tank.save(registries);
                entry.putLong("Pos", pos.asLong());
                list.add(entry);
            }
        });
        tag.put("Tanks", list);
        return tag;
    }
}
