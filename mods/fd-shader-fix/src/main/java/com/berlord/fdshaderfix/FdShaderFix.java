package com.berlord.fdshaderfix;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * fdlib loads its registered post-shader chains during the client resource reload
 * (FDPostShadersReloadableResourceListener.initializeShaders -> loadPostChain ->
 * reads shaders/post/<name>.json via the ResourceManager). That load has an
 * intermittent race: sometimes loadPostChain gets a FileNotFoundException for a
 * post-shader resource that DOES exist in the jar. fdlib has no per-chain error
 * handling -- the first failure is caught and rethrown as
 * RuntimeException("Failed to load shaders"), which hard-crashes the game.
 *
 * Result: packs using qliphoth-awakening (fdbosses), cinematic-cataclysm, or
 * anvilcraft's bloom (all register fdlib post-chains) crash on ~40% of launches
 * with the same mod set -- a pure timing race, not a missing file.
 *
 * This mod is purely a single client-side mixin ({@link com.berlord.fdshaderfix.mixin.FDPostShadersMixin}):
 * it wraps fdlib's no-arg initializeShaders() in a try/catch so a race-induced
 * failure is logged and the post-shaders are skipped for that reload, instead of
 * crashing. On the common (non-race) launch everything loads normally.
 *
 * Requires fdlib (declared in neoforge.mods.toml); the mod does nothing without it.
 */
@Mod(FdShaderFix.MOD_ID)
public class FdShaderFix {
    public static final String MOD_ID = "fdshaderfix";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FdShaderFix() {
        LOGGER.info("[{}] loaded; fdlib post-shader init is now crash-resilient", MOD_ID);
    }
}
