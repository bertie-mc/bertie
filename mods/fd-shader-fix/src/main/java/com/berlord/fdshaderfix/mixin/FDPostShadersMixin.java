package com.berlord.fdshaderfix.mixin;

import com.berlord.fdshaderfix.FdShaderFix;
import com.berlord.fdshaderfix.logic.ShaderInitialization;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

/** Prevents a failed fdlib post-shader reload from crashing the client. */
@Mixin(targets = "com.finderfeed.fdlib.systems.post_shaders.FDPostShadersReloadableResourceListener", remap = false)
public class FDPostShadersMixin {

    @WrapMethod(method = "initializeShaders")
    private static void fdshaderfix$resilientInit(Operation<Void> original) {
        ShaderInitialization.run(original::call, failure -> FdShaderFix.LOGGER.warn(
                "[{}] fdlib post-shader init failed; skipping post-shaders for this reload. Cause: {}",
                FdShaderFix.MOD_ID, failure.toString()));
    }
}
