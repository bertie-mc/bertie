package io.github.bertie_mc.bertieprogression.worldgen;

import io.github.bertie_mc.bertieprogression.RemovedItems;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Removes withdrawn Pastel placements and fixed inventory contents as templates are loaded. */
public final class PastelStructureRemoval {
    private PastelStructureRemoval() {}

    private static boolean removed(String name) {
        if (!name.startsWith("pastel:")) return false;
        ResourceLocation id = ResourceLocation.tryParse(name.replace("_wall_head", "_head"));
        return id != null && RemovedItems.isRemoved(id);
    }

    public static void clean(CompoundTag template) {
        Set<Integer> clearedStates = new HashSet<>();
        cleanPalette(template.getList("palette", Tag.TAG_COMPOUND), clearedStates);
        for (Tag palette : template.getList("palettes", Tag.TAG_LIST)) {
            cleanPalette((ListTag) palette, clearedStates);
        }
        for (Tag value : template.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag block = (CompoundTag) value;
            if (clearedStates.contains(block.getInt("state"))) block.remove("nbt");
        }
        cleanContents(template);
    }

    private static void cleanPalette(ListTag palette, Set<Integer> cleared) {
        for (int i = 0; i < palette.size(); i++) {
            CompoundTag state = palette.getCompound(i);
            if (removed(state.getString("Name"))) {
                state.putString("Name", "minecraft:air");
                state.remove("Properties");
                cleared.add(i);
            }
        }
    }

    private static void cleanContents(Tag value) {
        if (value instanceof ListTag list) {
            for (int i = list.size() - 1; i >= 0; i--) {
                Tag child = list.get(i);
                if (child instanceof CompoundTag compound && removed(compound.getString("id"))) {
                    list.remove(i);
                } else {
                    cleanContents(child);
                }
            }
        } else if (value instanceof CompoundTag compound) {
            for (String key : Set.copyOf(compound.getAllKeys())) {
                Tag child = compound.get(key);
                if (child instanceof CompoundTag stack && removed(stack.getString("id"))) {
                    compound.remove(key);
                } else {
                    cleanContents(child);
                }
            }
        }
    }
}
