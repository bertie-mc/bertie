package com.berlord.hephaestusarchitecture.structure;

import com.berlord.hephaestusarchitecture.HephaestusArchitecture;
import com.stal111.forbidden_arcanus.common.block.HephaestusForgeBlock;
import com.stal111.forbidden_arcanus.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.List;

final class StructureTemplateLayoutLoader {

    private StructureTemplateLayoutLoader() {
    }

    static ForgeLayout load(ServerLevel level, StructureTemplate template, int tier) {
        CompoundTag root = template.save(new CompoundTag());
        ListTag palette = firstPalette(root);
        ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
        List<BlockState> states = readPalette(level, palette);

        BlockPos anchor = null;
        int forgeMarkers = 0;

        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompound(i);
            BlockState state = states.get(entry.getInt("state"));
            if (state.getBlock() instanceof HephaestusForgeBlock) {
                anchor = readPos(entry);
                forgeMarkers++;
            }
        }

        if (forgeMarkers != 1 || anchor == null) {
            throw new IllegalArgumentException("Tier " + tier
                    + " template must contain exactly one Forbidden & Arcanus Hephaestus Forge block; found "
                    + forgeMarkers);
        }

        List<ForgeLayout.Requirement> requirements = new ArrayList<>();
        List<BlockPos> pedestals = new ArrayList<>();

        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompound(i);
            BlockState state = states.get(entry.getInt("state"));
            BlockPos relative = readPos(entry).subtract(anchor);

            if (state.getBlock() instanceof HephaestusForgeBlock || state.is(Blocks.STRUCTURE_VOID)) {
                continue;
            }
            if (state.is(ModTags.Blocks.PEDESTALS)) {
                pedestals.add(relative.immutable());
                requirements.add(ForgeLayout.Requirement.pedestal(relative));
            } else {
                requirements.add(ForgeLayout.Requirement.exact(relative, state));
            }
        }

        if (pedestals.isEmpty()) {
            HephaestusArchitecture.LOGGER.warn("Tier {} structure template contains no pedestals", tier);
        }

        return new ForgeLayout(tier, requirements, pedestals);
    }

    private static ListTag firstPalette(CompoundTag root) {
        if (root.contains("palettes", Tag.TAG_LIST)) {
            ListTag palettes = root.getList("palettes", Tag.TAG_LIST);
            if (palettes.isEmpty()) {
                throw new IllegalArgumentException("Structure template has an empty palettes list");
            }
            return palettes.getList(0);
        }
        return root.getList("palette", Tag.TAG_COMPOUND);
    }

    private static List<BlockState> readPalette(ServerLevel level, ListTag palette) {
        var blockLookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        List<BlockState> states = new ArrayList<>(palette.size());

        for (int i = 0; i < palette.size(); i++) {
            states.add(NbtUtils.readBlockState(blockLookup, palette.getCompound(i)));
        }
        return states;
    }

    private static BlockPos readPos(CompoundTag entry) {
        ListTag pos = entry.getList("pos", Tag.TAG_INT);
        return new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2));
    }
}
