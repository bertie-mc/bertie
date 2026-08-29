package io.github.bertie_mc.alexscavesworldgenfix.mixin.alexscaves;

import io.github.bertie_mc.alexscavesworldgenfix.logic.CaveBiomePin;
import io.github.bertie_mc.alexscavesworldgenfix.worldgen.AlexsCavesBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Writes the Magnetic Caves biome into the chunk the way every other Alex's Caves cave does.
 *
 * <p>Six cave types inherit {@code replaceBiomes} from
 * {@code AbstractCaveGenerationStructurePiece}; four of them call it at the end of
 * {@code postProcess} - Dino Bowl and Cake Cave with 32, Acid Pit with 20, Ocean Trench with 16.
 * Ferrocave and Forlorn Canyon never do.
 *
 * <p>Without that call the cave's biome is whatever the noise biome source sampled, and Alex's
 * Caves places its biomes on a climate window that includes {@code depth}. Where the sampled depth
 * leaves the window the biome reverts to the vanilla underground one, so the roof and upper walls
 * are Alex's Caves and the floor and lower walls are vanilla blocks decorated by vanilla features -
 * a flat horizontal seam across the cavern. The four caves that pin their biome overwrite that
 * boundary before it is visible; Ferrocave and Forlorn Canyon leave the seam intact.
 *
 * <p>Harmless if Alex's Caves adds the call itself: {@code replaceBiomes} only assigns section
 * biome containers, so running it twice writes the same value twice.
 */
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.level.structure.piece.FerrocaveStructurePiece", remap = false)
public abstract class FerrocaveStructurePieceMixin {

    @Shadow
    public abstract void replaceBiomes(WorldGenLevel level, ResourceKey<Biome> biome, int offset);

    // require = 1 against the config's defaultRequire of 0. Alex's Caves absent means this class
    // is absent and the whole mixin is skipped; Alex's Caves present but reshaped has to be loud,
    // because the failure mode otherwise is a silent no-op and a seam nobody can explain.
    @Inject(method = "postProcess", at = @At("TAIL"), require = 1)
    private void alexscavesworldgenfix$pinMagneticCavesBiome(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            BlockPos pos,
            CallbackInfo callback) {
        BoundingBox cave = ((StructurePiece) (Object) this).getBoundingBox();
        replaceBiomes(
                level,
                AlexsCavesBiomes.MAGNETIC_CAVES,
                CaveBiomePin.offsetBelowSeaLevel(level.getSeaLevel(), cave.maxY()));
    }
}
