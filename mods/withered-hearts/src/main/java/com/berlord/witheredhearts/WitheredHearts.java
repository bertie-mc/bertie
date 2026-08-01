package com.berlord.witheredhearts;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Client-side HUD refinement for finite Wither damage. */
@Mod(value = WitheredHearts.MOD_ID, dist = Dist.CLIENT)
public class WitheredHearts {
    public static final String MOD_ID = "witheredhearts";

    public WitheredHearts(IEventBus ignored) {
    }
}
