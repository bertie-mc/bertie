package io.github.bertie_mc.explosiveenhancement;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Client-only explosion particle replacement. */
@Mod(value = ExplosiveEnhancement.MOD_ID, dist = Dist.CLIENT)
public class ExplosiveEnhancement {
    public static final String MOD_ID = "explosiveenhancement";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ExplosiveEnhancement(IEventBus ignored) {
        ExplosiveConfig.load();
        LOGGER.info("Explosive Enhancement loaded.");
    }
}
