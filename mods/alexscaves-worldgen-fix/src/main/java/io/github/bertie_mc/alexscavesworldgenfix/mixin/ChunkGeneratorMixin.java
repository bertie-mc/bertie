package io.github.bertie_mc.alexscavesworldgenfix.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bertie_mc.alexscavesworldgenfix.AlexsCavesWorldgenFix;
import io.github.bertie_mc.alexscavesworldgenfix.logic.BiomeDecorationFailure;
import io.github.bertie_mc.alexscavesworldgenfix.logic.TolerantIndexList;
import io.github.bertie_mc.alexscavesworldgenfix.worldgen.NoOpPlacedFeature;
import java.util.List;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keeps a chunk's decoration going when the feature sorter cannot identify one of its features.
 *
 * <p><b>Priority 1500, deliberately.</b> Higher priority applies LATER, so by the time these
 * injectors are installed the other mixins on {@code applyBiomeDecoration} - alexscaves'
 * {@code @Redirect}, plus wover, bclib and moonlight, all at the default 1000 - are already baked
 * into the body being wrapped. At the default priority the wrap could be installed first and miss
 * the very redirect it exists to contain.
 */
@Mixin(value = ChunkGenerator.class, priority = 1500)
public abstract class ChunkGeneratorMixin {

    /**
     * Replaces the step's feature list with one that cannot throw on a bad index.
     *
     * <p>The decoration loop looks a feature up by an index taken from
     * {@code StepFeatureData#indexMapping}, an identity lookup that answers {@code -1} for anything
     * it has not been shown. Alex's Caves' biomes declare features in the {@code STRONGHOLDS} step,
     * which nothing else in the pack populates, so that step's sorted list is empty and every
     * lookup into it misses. Alex's Caves' own clamp turns the {@code -1} into {@code 0}, and
     * {@code get(0)} on an empty list throws.
     *
     * <p>That single throw used to cost the whole rest of the chunk: {@code STRONGHOLDS} is step 5
     * of 11, so {@code UNDERGROUND_ORES}, {@code UNDERGROUND_DECORATION}, {@code FLUID_SPRINGS},
     * {@code VEGETAL_DECORATION} and {@code TOP_LAYER_MODIFICATION} never ran. In a Primordial Cave
     * that is every tree; in Magnetic Caves the crystals and the ore pass; in Forlorn Hollows the
     * block palette and the ruins.
     *
     * <p>Now the unidentifiable feature alone is skipped and the remaining steps still run.
     *
     * <p><b>Why here and not on the {@code get} itself.</b> Alex's Caves already owns that
     * instruction with a {@code @Redirect}, and one injector owns an instruction. Wrapping the call
     * that produces the list leaves their redirect intact - it clamps against this list instead,
     * and this list answers rather than throws.
     */
    @WrapOperation(
            method = "applyBiomeDecoration",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/biome/FeatureSorter$StepFeatureData;"
                                    + "features()Ljava/util/List;"))
    private List<PlacedFeature> alexscavesworldgenfix$tolerateAnUnknownFeatureIndex(
            FeatureSorter.StepFeatureData step, Operation<List<PlacedFeature>> original) {
        return TolerantIndexList.wrap(
                original.call(step), NoOpPlacedFeature.get(), AlexsCavesWorldgenFix::recordSubstitutedFeature);
    }

    /**
     * Backstop for a clamp failure that reaches the top of the method anyway.
     *
     * <p>The wrap above removes the known route to it, so this should now stay silent. It is kept
     * because it is the difference between an under-decorated chunk and a dead worldgen thread, and
     * a future Alex's Caves build could clamp somewhere this mod does not cover.
     */
    @WrapMethod(method = "applyBiomeDecoration")
    private void alexscavesworldgenfix$surviveClampFailure(
            WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager, Operation<Void> original) {
        try {
            original.call(level, chunk, structureManager);
        } catch (Throwable thrown) {
            // Everything that is not the clamp failure keeps crashing exactly as it did before.
            if (!BiomeDecorationFailure.isAlexsCavesClamp(thrown)) {
                throw thrown;
            }
            AlexsCavesWorldgenFix.recordAbsorbedFailure(chunk.getPos(), thrown);
        }
    }
}
