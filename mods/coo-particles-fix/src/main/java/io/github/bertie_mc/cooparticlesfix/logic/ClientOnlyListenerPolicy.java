package io.github.bertie_mc.cooparticlesfix.logic;

import net.neoforged.api.distmarker.Dist;

/** Limits the workaround to CooParticles' client-only test listener on physical servers. */
public final class ClientOnlyListenerPolicy {
    static final String CLIENT_ONLY_TEST_LISTENER = "cn.coostack.cooparticlesapi.listeners.TestBlockPlayerPathListener";

    private ClientOnlyListenerPolicy() {}

    public static boolean shouldSkip(Dist dist, String listenerClassName) {
        return dist == Dist.DEDICATED_SERVER && CLIENT_ONLY_TEST_LISTENER.equals(listenerClassName);
    }
}
