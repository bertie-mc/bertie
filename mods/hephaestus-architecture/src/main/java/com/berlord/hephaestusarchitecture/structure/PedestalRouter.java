package com.berlord.hephaestusarchitecture.structure;

import com.berlord.hephaestusarchitecture.mixin.HephaestusForgeBlockEntityAccessor;
import com.stal111.forbidden_arcanus.common.block.HephaestusForgeBlock;
import com.stal111.forbidden_arcanus.common.block.entity.PedestalBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ForgeDataCache;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class PedestalRouter {

    private PedestalRouter() {
    }

    /**
     * Routes a pedestal update only when that pedestal occupies an input marker in
     * the currently valid tier layout.
     */
    public static void route(ServerLevel level, BlockPos pedestalPos, ItemStack stack) {
        findForge(level, pedestalPos).ifPresent(forge -> forge.updatePedestalStack(pedestalPos, stack));
    }

    public static void rebuild(ServerLevel level,
                               BlockPos forgePos,
                               HephaestusForgeBlockEntity forge,
                               ForgeLayout.Match match) {
        HephaestusForgeBlockEntityAccessor accessor = (HephaestusForgeBlockEntityAccessor) forge;
        ForgeDataCache cache = accessor.hephaestusarchitecture$getDataCache();
        cache.cachedIngredients().clear();

        for (BlockPos pedestalPos : match.pedestalPositions(forgePos)) {
            if (level.getBlockEntity(pedestalPos) instanceof PedestalBlockEntity pedestal) {
                cache.setIngredient(pedestalPos, pedestal.getStack());
            }
        }

        accessor.hephaestusarchitecture$onDataChanged(level.registryAccess());
    }

    public static void clear(ServerLevel level, HephaestusForgeBlockEntity forge) {
        HephaestusForgeBlockEntityAccessor accessor = (HephaestusForgeBlockEntityAccessor) forge;
        accessor.hephaestusarchitecture$getDataCache().cachedIngredients().clear();
        accessor.hephaestusarchitecture$onDataChanged(level.registryAccess());
    }

    private static Optional<HephaestusForgeBlockEntity> findForge(ServerLevel level, BlockPos pedestalPos) {
        record Candidate(HephaestusForgeBlockEntity forge, double distance) {
        }

        java.util.ArrayList<Candidate> candidates = new java.util.ArrayList<>();

        for (int tier = 1; tier <= 5; tier++) {
            List<BlockPos> offsets = ForgeLayouts.candidatePedestalOffsets(level, tier);

            for (BlockPos offset : offsets) {
                BlockPos forgePos = pedestalPos.subtract(offset);
                if (!(level.getBlockEntity(forgePos) instanceof HephaestusForgeBlockEntity forge)) {
                    continue;
                }
                if (!(level.getBlockState(forgePos).getBlock() instanceof HephaestusForgeBlock forgeBlock)
                        || forgeBlock.getLevel().getAsInt() != tier) {
                    continue;
                }

                ForgeLayout.Match match = ForgeLayouts.match(level, forgePos, tier);
                if (match != null && match.pedestalPositions(forgePos).contains(pedestalPos)) {
                    candidates.add(new Candidate(forge, forgePos.distSqr(pedestalPos)));
                }
            }
        }

        return candidates.stream()
                .min(java.util.Comparator.comparingDouble(Candidate::distance))
                .map(Candidate::forge);
    }
}
