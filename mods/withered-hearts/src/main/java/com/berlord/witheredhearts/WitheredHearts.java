package com.berlord.witheredhearts;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Withered Hearts — client-side HUD refinement.
 *
 * <p>While the local player has the Wither effect, vanilla swaps the ENTIRE
 * health bar to the dark "withered" heart sprite. This mod narrows that:
 * only the leading half-hearts that Wither will actually drain before it
 * expires are drawn dark; the rest stay normal red. The dark segment shrinks
 * as the Wither timer counts down.
 *
 * <p>All behavior lives in {@code GuiHeartsMixin}; this class only declares the
 * mod and is marked client-only so the loader never attempts to apply the
 * client GUI mixin on a dedicated server.
 */
@Mod(value = WitheredHearts.MOD_ID, dist = Dist.CLIENT)
public class WitheredHearts {
    public static final String MOD_ID = "witheredhearts";

    public WitheredHearts(IEventBus modBus) {
        // Mixin-only mod; nothing to register on the mod event bus.
    }
}
