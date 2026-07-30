package com.berlord.hephaestusarchitecture.structure;

import com.berlord.hephaestusarchitecture.HephaestusArchitecture;
import com.stal111.forbidden_arcanus.common.block.ModBlockPatterns;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ForgeLayouts {

    /**
     * The eight native pedestal positions around the central forge.
     * They are symmetric under all four rotations.
     */
    private static final List<BlockPos> NATIVE_PEDESTALS = List.of(
            new BlockPos(-2, 0, -3),
            new BlockPos(2, 0, -3),
            new BlockPos(-3, 0, -2),
            new BlockPos(3, 0, -2),
            new BlockPos(-3, 0, 2),
            new BlockPos(3, 0, 2),
            new BlockPos(-2, 0, 3),
            new BlockPos(2, 0, 3)
    );

    private static final Map<StructureTemplate, ForgeLayout> CACHE =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Set<Integer> MISSING_TEMPLATE_WARNINGS =
            Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Set<Integer> INVALID_TEMPLATE_WARNINGS =
            Collections.synchronizedSet(new LinkedHashSet<>());

    private ForgeLayouts() {
    }

    public static @Nullable ForgeLayout.Match match(ServerLevel level, BlockPos forgePos, int tier) {
        Optional<ForgeLayout> custom = custom(level, tier);
        if (custom.isPresent()) {
            return custom.get().match(level, forgePos);
        }

        if (ModBlockPatterns.BASE_HEPHAESTUS_PATTERN.find(level, forgePos.below()) == null) {
            return null;
        }
        return new ForgeLayout.Match(Rotation.NONE, NATIVE_PEDESTALS);
    }

    public static List<BlockPos> candidatePedestalOffsets(ServerLevel level, int tier) {
        Optional<ForgeLayout> custom = custom(level, tier);
        if (custom.isEmpty()) {
            return NATIVE_PEDESTALS;
        }

        Set<BlockPos> offsets = new LinkedHashSet<>();
        for (Rotation rotation : Rotation.values()) {
            for (BlockPos offset : custom.get().pedestalOffsets()) {
                offsets.add(ForgeLayout.rotate(offset, rotation).immutable());
            }
        }
        return List.copyOf(offsets);
    }

    private static Optional<ForgeLayout> custom(ServerLevel level, int tier) {
        if (tier <= 1) {
            return Optional.empty();
        }

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                HephaestusArchitecture.MOD_ID, "hephaestus_forge/tier_" + tier);
        Optional<StructureTemplate> template = level.getStructureManager().get(id);

        if (template.isEmpty()) {
            if (MISSING_TEMPLATE_WARNINGS.add(tier)) {
                HephaestusArchitecture.LOGGER.warn(
                        "No {} structure template was found; tier {} keeps the native F&A layout until it is supplied",
                        id, tier);
            }
            return Optional.empty();
        }

        try {
            return Optional.of(CACHE.computeIfAbsent(template.get(),
                    structure -> StructureTemplateLayoutLoader.load(level, structure, tier)));
        } catch (RuntimeException exception) {
            if (INVALID_TEMPLATE_WARNINGS.add(tier)) {
                HephaestusArchitecture.LOGGER.error(
                        "Invalid tier {} Hephaestus Forge structure {}; falling back to the native layout",
                        tier, id, exception);
            }
            return Optional.empty();
        }
    }
}
