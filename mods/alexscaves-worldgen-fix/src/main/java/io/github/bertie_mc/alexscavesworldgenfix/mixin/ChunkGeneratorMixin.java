package io.github.bertie_mc.alexscavesworldgenfix.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.bertie_mc.alexscavesworldgenfix.AlexsCavesWorldgenFix;
import io.github.bertie_mc.alexscavesworldgenfix.logic.BiomeDecorationFailure;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Lets a chunk finish when Alex's Caves' decoration clamp throws on an empty feature list.
 *
 * <p><b>Priority 1500, deliberately.</b> Higher priority applies LATER, so by the time this wrapper
 * is installed the other mixins on {@code applyBiomeDecoration} - alexscaves' {@code @Redirect},
 * plus wover, bclib and moonlight, all at the default 1000 - are already baked into the body being
 * wrapped. At the default priority the wrap could be installed first and miss the very redirect it
 * exists to contain.
 *
 * <p><b>Why the whole method and not the {@code List.get} itself.</b> A second {@code @Redirect} or
 * a {@code @WrapOperation} on that instruction would collide with alexscaves' own redirect - one
 * injector owns an instruction. Cancelling their clamp handler is no better: it has to return
 * <i>something</i>, and a null {@code PlacedFeature} just moves the crash one line down into
 * {@code placeWithBiomeCheck}. Wrapping the method is the only seam that composes.
 */
@Mixin(value = ChunkGenerator.class, priority = 1500)
public abstract class ChunkGeneratorMixin {

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
