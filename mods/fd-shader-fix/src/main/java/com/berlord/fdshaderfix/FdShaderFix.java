package com.berlord.fdshaderfix;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Makes fdlib post-shader reload failures non-fatal. */
@Mod(value = FdShaderFix.MOD_ID, dist = Dist.CLIENT)
public class FdShaderFix {
    public static final String MOD_ID = "fdshaderfix";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FdShaderFix() {
        LOGGER.info("[{}] loaded; fdlib post-shader init is now crash-resilient", MOD_ID);
    }
}
