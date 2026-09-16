package io.github.bertie_mc.creeperoverhaulfix.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Creeper Overhaul builds its tiny cactus from bare {@code Properties.of()}, so the block keeps
 * {@code canOcclude}. Every vanilla bush clears it through {@code noCollission()}, which is why only
 * this one turns the ground under it into a hole once another mod widens its shape.
 *
 * <p>The properties are still mutable here: {@code Block}'s constructor builds the state definition
 * after {@code BlockBehaviour}'s returns, and that is where each state copies {@code canOcclude}.
 */
@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {

    @Unique
    private static final String TINY_CACTUS = "tech.thatgravyboat.creeperoverhaul.common.block.TinyCactusBlock";

    @Inject(method = "<init>", at = @At("RETURN"))
    private void creeperoverhaulfix$clearTinyCactusOcclusion(
            BlockBehaviour.Properties properties, CallbackInfo callback) {
        if (TINY_CACTUS.equals(this.getClass().getName())) {
            properties.noOcclusion();
        }
    }
}
