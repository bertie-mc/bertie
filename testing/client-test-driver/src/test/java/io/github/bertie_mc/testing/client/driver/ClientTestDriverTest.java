package io.github.bertie_mc.testing.client.driver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

final class ClientTestDriverTest {
    @Test
    void startupWaitsForArmingAndOverlayRemovalOnly() {
        assertFalse(ClientTestDriver.isStartupReady(false, false));
        assertFalse(ClientTestDriver.isStartupReady(true, true));
        assertTrue(ClientTestDriver.isStartupReady(true, false));
    }

    @Test
    void initializationFailureBecomesAReportedOutcome() {
        var suiteRan = new AtomicBoolean();
        var failure = new IllegalStateException("initialization failed");

        var outcomes = ClientTestDriver.collectReportedOutcomes(
                () -> {
                    throw failure;
                },
                ignored -> suiteRan.set(true));

        assertFalse(suiteRan.get());
        assertTrue(outcomes.getFirst().failed());
        assertSame(failure, outcomes.getFirst().failure());
    }
}
