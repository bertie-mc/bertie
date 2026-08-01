package com.berlord.hephaestusarchitecture;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(HephaestusArchitecture.MOD_ID)
public final class HephaestusArchitecture {

    public static final String MOD_ID = "hephaestusarchitecture";
    public static final Logger LOGGER = LoggerFactory.getLogger("HephaestusArchitecture");

    public HephaestusArchitecture(IEventBus ignored) {
        LOGGER.info("Tiered Hephaestus Forge structure support enabled");
    }
}
