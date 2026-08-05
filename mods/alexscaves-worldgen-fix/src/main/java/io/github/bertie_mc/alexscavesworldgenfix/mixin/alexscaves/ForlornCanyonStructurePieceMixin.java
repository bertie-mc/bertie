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
 * Writes the Forlorn Hollows biome into the chunk, the other half of the pair Alex's Caves missed.
 *
 * <p>See {@link FerrocaveStructurePieceMixin} for why this is needed and why running it twice is
 * safe.
 */
@Mixin(
        targets = "com.github.alexmodguy.alexscaves.server.level.structure.piece."
                + "ForlornCanyonStructurePiece",
        remap = false)
public abstract class ForlornCanyonStructurePieceMixin {

    @Shadow
    public abstract void replaceBiomes(WorldGenLevel level, ResourceKey<Biome> biome, int offset);

    // See FerrocaveStructurePieceMixin for why this overrides the config's defaultRequire of 0.
    @Inject(method = "postProcess", at = @At("TAIL"), require = 1)
    private void alexscavesworldgenfix$pinForlornHollowsBiome(
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
                AlexsCavesBiomes.FORLORN_HOLLOWS,
                CaveBiomePin.offsetBelowSeaLevel(level.getSeaLevel(), cave.maxY()));
    }
}
