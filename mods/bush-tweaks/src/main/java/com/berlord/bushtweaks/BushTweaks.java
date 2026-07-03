package com.berlord.bushtweaks;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(BushTweaks.MOD_ID)
public class BushTweaks {
    public static final String MOD_ID = "bushtweaks";

    public BushTweaks(IEventBus modBus) {
        // Mixin-only mod; all behavior is in the mixin package.
    }
}
