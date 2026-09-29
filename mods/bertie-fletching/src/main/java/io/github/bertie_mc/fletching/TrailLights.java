package io.github.bertie_mc.fletching;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class TrailLights extends SavedData {
    private final Map<BlockPos, Long> lights = new HashMap<>();

    public static TrailLights get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(new Factory<>(TrailLights::new, TrailLights::load, null), "bertie_arrow_lights");
    }

    public void place(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 14));
        lights.put(pos.immutable(), level.getGameTime() + 200);
        setDirty();
    }

    public static void tick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            TrailLights data = get(level);
            var it = data.lights.entrySet().iterator();
            while (it.hasNext()) {
                var entry = it.next();
                if (entry.getValue() <= level.getGameTime() && level.hasChunkAt(entry.getKey())) {
                    if (level.getBlockState(entry.getKey()).is(Blocks.LIGHT)) level.removeBlock(entry.getKey(), false);
                    it.remove();
                    data.setDirty();
                }
            }
        }
    }

    private static TrailLights load(CompoundTag tag, HolderLookup.Provider registries) {
        TrailLights data = new TrailLights();
        for (var entry : tag.getList("Lights", 10)) {
            var t = (CompoundTag) entry;
            data.lights.put(BlockPos.of(t.getLong("Pos")), t.getLong("Until"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        lights.forEach((pos, until) -> {
            CompoundTag t = new CompoundTag();
            t.putLong("Pos", pos.asLong());
            t.putLong("Until", until);
            list.add(t);
        });
        tag.put("Lights", list);
        return tag;
    }
}
