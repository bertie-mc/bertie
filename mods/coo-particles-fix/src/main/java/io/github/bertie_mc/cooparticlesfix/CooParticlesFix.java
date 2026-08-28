package io.github.bertie_mc.cooparticlesfix;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CooParticlesFix.MOD_ID)
public final class CooParticlesFix {
    public static final String MOD_ID = "cooparticlesfix";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CooParticlesFix(IEventBus ignored) {}
}
