package io.github.bertie_mc.cooparticlesfix.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.neoforged.api.distmarker.Dist;
import org.junit.jupiter.api.Test;

class ClientOnlyListenerPolicyTest {
    @Test
    void skipsKnownClientOnlyListenerOnDedicatedServer() {
        assertTrue(ClientOnlyListenerPolicy.shouldSkip(
                Dist.DEDICATED_SERVER, ClientOnlyListenerPolicy.CLIENT_ONLY_TEST_LISTENER));
    }

    @Test
    void keepsKnownListenerOnClient() {
        assertFalse(
                ClientOnlyListenerPolicy.shouldSkip(Dist.CLIENT, ClientOnlyListenerPolicy.CLIENT_ONLY_TEST_LISTENER));
    }

    @Test
    void keepsEveryOtherListenerOnDedicatedServer() {
        assertFalse(ClientOnlyListenerPolicy.shouldSkip(
                Dist.DEDICATED_SERVER, "cn.coostack.usefulmagic.listener.WorldDragonListener"));
    }
}
