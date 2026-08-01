package io.github.bertie_mc.frozenregfix;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Ensures Immersive Armors initializes its armor materials before registry freeze. */
@Mod(FrozenRegFix.MOD_ID)
public class FrozenRegFix {
    public static final String MOD_ID = "frozenregfix";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final String[] TARGETS = {
        "immersive_armors.Items",
    };

    private static volatile boolean modConstructed = false;
    private static volatile boolean done = false;

    public FrozenRegFix() {
        modConstructed = true;
        LOGGER.info("[{}] loaded", MOD_ID);
    }

    /** Force each target once after mod construction, while its target registry is writable. */
    public static synchronized void forceTargets(String source) {
        if (done || !modConstructed) {
            return;
        }
        done = true;
        for (String target : TARGETS) {
            try {
                Class.forName(target, true, FrozenRegFix.class.getClassLoader());
                LOGGER.info("[{}] initialized {} from {}", MOD_ID, target, source);
            } catch (ClassNotFoundException e) {
                LOGGER.info("[{}] target {} not present, skipping", MOD_ID, target);
            } catch (Throwable t) {
                LOGGER.error("[{}] failed to force-initialize {}", MOD_ID, target, t);
            }
        }
    }
}
