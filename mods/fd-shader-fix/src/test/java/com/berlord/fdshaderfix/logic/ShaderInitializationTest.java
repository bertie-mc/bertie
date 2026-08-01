package com.berlord.fdshaderfix.logic;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShaderInitializationTest {

    @Test
    void preservesSuccessfulInitialization() {
        AtomicBoolean initialized = new AtomicBoolean();
        AtomicBoolean reported = new AtomicBoolean();

        ShaderInitialization.run(() -> initialized.set(true), failure -> reported.set(true));

        assertTrue(initialized.get());
        assertFalse(reported.get());
    }

    @Test
    void reportsAndSuppressesInitializationFailure() {
        RuntimeException failure = new RuntimeException("shader load failed");
        AtomicReference<Throwable> reported = new AtomicReference<>();

        ShaderInitialization.run(() -> {
            throw failure;
        }, reported::set);

        assertSame(failure, reported.get());
    }
}
