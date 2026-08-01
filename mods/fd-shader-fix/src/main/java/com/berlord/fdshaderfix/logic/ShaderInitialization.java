package com.berlord.fdshaderfix.logic;

import java.util.function.Consumer;

/** Failure boundary used around fdlib's post-shader initialization. */
public final class ShaderInitialization {

    private ShaderInitialization() {
    }

    public static void run(Attempt initialization, Consumer<Throwable> failureHandler) {
        try {
            initialization.run();
        } catch (Throwable failure) {
            failureHandler.accept(failure);
        }
    }

    @FunctionalInterface
    public interface Attempt {
        void run() throws Throwable;
    }
}
