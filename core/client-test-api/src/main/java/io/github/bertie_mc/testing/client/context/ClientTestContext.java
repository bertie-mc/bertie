package io.github.bertie_mc.testing.client.context;

import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.TestInput;
import io.github.bertie_mc.testing.client.world.TestWorldBuilder;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.Nullable;

/**
 * State-oriented operations available to a {@link ClientTest} method.
 *
 * <p>Unless otherwise specified, methods may only be called from the client-test thread.
 */
public interface ClientTestContext {
    /** A timeout value that disables the timeout of a wait operation. */
    int NO_TIMEOUT = -1;

    /** The default timeout for state and screen waits, in ticks. */
    int DEFAULT_TIMEOUT_TICKS = 200;

    /**
     * Returns the input simulator owned by this test context.
     *
     * @return the test input simulator
     */
    TestInput input();

    /** Advances the client, and the current logical server if present, by one tick. */
    void waitTick();

    /**
     * Advances the game by the requested number of ticks.
     *
     * @param ticks the non-negative number of ticks to wait
     * @throws IllegalArgumentException if {@code ticks} is negative
     */
    void waitTicks(int ticks);

    /**
     * Waits for a client predicate using {@link #DEFAULT_TIMEOUT_TICKS}.
     *
     * @param condition the predicate to evaluate on the render thread
     * @return the number of ticks waited, possibly zero
     * @throws AssertionError if the predicate is still false at the timeout
     */
    int waitFor(Predicate<Minecraft> condition);

    /**
     * Waits for a client predicate.
     *
     * @param condition the predicate to evaluate on the render thread
     * @param timeoutTicks a positive timeout in ticks, or {@link #NO_TIMEOUT}
     * @return the number of ticks waited, possibly zero
     * @throws IllegalArgumentException if the timeout is neither positive nor {@link #NO_TIMEOUT}
     * @throws AssertionError if the predicate is still false at the timeout
     */
    int waitFor(Predicate<Minecraft> condition, int timeoutTicks);

    /**
     * Waits for a described client condition using {@link #DEFAULT_TIMEOUT_TICKS}.
     *
     * @param description a human-readable condition used in timeout failures
     * @param condition the predicate to evaluate on the render thread
     * @return the number of ticks waited, possibly zero
     * @throws AssertionError if the predicate is still false at the timeout
     */
    int waitFor(String description, Predicate<Minecraft> condition);

    /**
     * Waits for a described client condition.
     *
     * @param description a human-readable condition used in timeout failures
     * @param condition the predicate to evaluate on the render thread
     * @param timeoutTicks a positive timeout in ticks, or {@link #NO_TIMEOUT}
     * @return the number of ticks waited, possibly zero
     * @throws IllegalArgumentException if the timeout is neither positive nor {@link #NO_TIMEOUT}
     * @throws AssertionError if the predicate is still false at the timeout
     */
    int waitFor(String description, Predicate<Minecraft> condition, int timeoutTicks);

    /**
     * Waits for a screen using {@link #DEFAULT_TIMEOUT_TICKS}. A {@code null} type waits until no
     * screen is open.
     *
     * @param screenType the required screen type, or {@code null} for no screen
     * @return the number of ticks waited, possibly zero
     * @throws AssertionError if the required screen state is not reached
     */
    int waitForScreen(@Nullable Class<? extends Screen> screenType);

    /**
     * Waits for a screen. A {@code null} type waits until no screen is open.
     *
     * @param screenType the required screen type, or {@code null} for no screen
     * @param timeoutTicks a positive timeout in ticks, or {@link #NO_TIMEOUT}
     * @return the number of ticks waited, possibly zero
     * @throws IllegalArgumentException if the timeout is neither positive nor {@link #NO_TIMEOUT}
     * @throws AssertionError if the required screen state is not reached
     */
    int waitForScreen(@Nullable Class<? extends Screen> screenType, int timeoutTicks);

    /**
     * Sets the screen on the render thread. The supplier may return {@code null} to close the
     * current screen.
     *
     * @param screen a supplier evaluated on the render thread
     */
    void setScreen(Supplier<@Nullable Screen> screen);

    /**
     * Clicks the visible button whose translated label matches the given translation key.
     *
     * @param translationKey the button label's translation key
     * @throws AssertionError if no matching button is visible on the current screen
     * @see #tryClickScreenButton(String)
     */
    void clickScreenButton(String translationKey);

    /**
     * Attempts to click the visible button whose translated label matches the translation key.
     *
     * @param translationKey the button label's translation key
     * @return whether a matching button was found and clicked
     */
    boolean tryClickScreenButton(String translationKey);

    /**
     * Runs an action on the render thread and waits for it to complete. If called from the render
     * thread, the action runs directly.
     *
     * @param action the action to run
     * @param <E> the checked exception thrown by the action
     * @throws E if the action throws it
     */
    <E extends Throwable> void runOnClient(FailableConsumer<Minecraft, E> action) throws E;

    /**
     * Computes a value on the render thread and waits for it. If called from the render thread, the
     * function runs directly.
     *
     * @param function the function to run
     * @param <T> the returned value type
     * @param <E> the checked exception thrown by the function
     * @return the function result
     * @throws E if the function throws it
     */
    <T, E extends Throwable> T computeOnClient(FailableFunction<Minecraft, T, E> function) throws E;

    /**
     * Creates a fresh builder for an integrated world or in-process dedicated server.
     *
     * @return a new test world builder
     */
    TestWorldBuilder worldBuilder();

    /** Restores the game-option baseline captured after client initialization. */
    void restoreDefaultGameOptions();

    /**
     * Captures the next rendered frame in the test diagnostics directory.
     *
     * @param name a relative screenshot name, with or without the {@code .png} suffix
     * @return the absolute path of the written PNG file
     * @throws IllegalArgumentException if the name escapes the diagnostics directory
     */
    Path takeScreenshot(String name);
}
