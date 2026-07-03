package com.berlord.fdshaderfix.mixin;

import com.berlord.fdshaderfix.FdShaderFix;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Targeted by string (fdlib is not on the compile classpath). Wraps fdlib's
 * static, no-arg {@code initializeShaders()} so the intermittent
 * "Failed to load shaders" RuntimeException (a resource-reload race) is caught
 * and logged instead of crashing the client.
 *
 * Using a no-arg target means the wrapper signature needs no fdlib types --
 * {@code Operation<Void>} is all that is required.
 */
@Mixin(targets = "com.finderfeed.fdlib.systems.post_shaders.FDPostShadersReloadableResourceListener", remap = false)
public class FDPostShadersMixin {

    @WrapMethod(method = "initializeShaders")
    private static void fdshaderfix$resilientInit(Operation<Void> original) {
        try {
            original.call();
        } catch (Throwable t) {
            FdShaderFix.LOGGER.warn(
                "[{}] fdlib post-shader init failed this resource reload (likely the intermittent "
                    + "load race); skipping post-shaders for now instead of crashing. Cause: {}",
                FdShaderFix.MOD_ID, t.toString());
        }
    }
}
