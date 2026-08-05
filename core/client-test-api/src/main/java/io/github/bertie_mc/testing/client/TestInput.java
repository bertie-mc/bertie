package io.github.bertie_mc.testing.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.Function;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;

/**
 * Input delivered through Minecraft's keyboard and mouse handlers.
 *
 * <p>Unless otherwise specified, methods may only be called from the client-test thread. Holding
 * or releasing a key changes its simulated state immediately but does not wait for Minecraft to
 * react to that state.
 */
public interface TestInput {
    /**
     * Holds the currently bound key for a key mapping until it is released.
     *
     * @param keyMapping the bound key mapping to hold
     * @throws AssertionError if the mapping is unbound
     * @see #releaseKey(KeyMapping)
     */
    void holdKey(KeyMapping keyMapping);

    /**
     * Resolves a key mapping from the client options and holds its current binding.
     *
     * @param keyMappingGetter a function evaluated against the client options
     * @throws AssertionError if the mapping is unbound
     */
    void holdKey(Function<Options, KeyMapping> keyMappingGetter);

    /**
     * Holds a key or mouse button until {@link #releaseKey(InputConstants.Key)} is called.
     *
     * @param key the key or mouse button to hold
     */
    void holdKey(InputConstants.Key key);

    /**
     * Holds a GLFW key until {@link #releaseKey(int)} is called.
     *
     * @param keyCode the GLFW key code
     */
    void holdKey(int keyCode);

    /**
     * Holds a mouse button until {@link #releaseMouse(int)} is called.
     *
     * @param button the GLFW mouse button
     */
    void holdMouse(int button);

    /**
     * Holds left control, or left super on macOS, for {@link Screen#hasControlDown()}.
     *
     * @see #releaseControl()
     */
    void holdControl();

    /** Holds left shift for {@link Screen#hasShiftDown()}. */
    void holdShift();

    /** Holds left alt for {@link Screen#hasAltDown()}. */
    void holdAlt();

    /**
     * Releases the currently bound key for a key mapping if this input is holding it.
     *
     * @param keyMapping the bound key mapping to release
     * @throws AssertionError if the mapping is unbound
     */
    void releaseKey(KeyMapping keyMapping);

    /**
     * Resolves a key mapping from the client options and releases its current binding.
     *
     * @param keyMappingGetter a function evaluated against the client options
     * @throws AssertionError if the mapping is unbound
     */
    void releaseKey(Function<Options, KeyMapping> keyMappingGetter);

    /**
     * Releases a key or mouse button if this input is holding it.
     *
     * @param key the key or mouse button to release
     */
    void releaseKey(InputConstants.Key key);

    /**
     * Releases a GLFW key if this input is holding it.
     *
     * @param keyCode the GLFW key code
     */
    void releaseKey(int keyCode);

    /**
     * Releases a mouse button if this input is holding it.
     *
     * @param button the GLFW mouse button
     */
    void releaseMouse(int button);

    /** Releases the platform-specific key held by {@link #holdControl()}. */
    void releaseControl();

    /** Releases left shift if this input is holding it. */
    void releaseShift();

    /** Releases left alt if this input is holding it. */
    void releaseAlt();

    /**
     * Presses and releases a key mapping, then advances the game by one tick.
     *
     * @param keyMapping the bound key mapping to press
     * @throws AssertionError if the mapping is unbound
     */
    void pressKey(KeyMapping keyMapping);

    /**
     * Resolves and presses a key mapping, then advances the game by one tick.
     *
     * @param keyMappingGetter a function evaluated against the client options
     * @throws AssertionError if the mapping is unbound
     */
    void pressKey(Function<Options, KeyMapping> keyMappingGetter);

    /**
     * Presses and releases a key or mouse button, then advances the game by one tick.
     *
     * @param key the key or mouse button to press
     */
    void pressKey(InputConstants.Key key);

    /**
     * Presses and releases a GLFW key, then advances the game by one tick.
     *
     * @param keyCode the GLFW key code
     */
    void pressKey(int keyCode);

    /**
     * Presses and releases a mouse button, then advances the game by one tick.
     *
     * @param button the GLFW mouse button
     */
    void pressMouse(int button);

    /**
     * Holds a key mapping for a number of ticks and then releases it.
     *
     * @param keyMapping the bound key mapping to hold
     * @param ticks the non-negative number of ticks to hold it
     * @throws IllegalArgumentException if {@code ticks} is negative
     * @throws AssertionError if the mapping is unbound
     */
    void holdKeyFor(KeyMapping keyMapping, int ticks);

    /**
     * Resolves and holds a key mapping for a number of ticks, then releases it.
     *
     * @param keyMappingGetter a function evaluated against the client options
     * @param ticks the non-negative number of ticks to hold it
     * @throws IllegalArgumentException if {@code ticks} is negative
     * @throws AssertionError if the mapping is unbound
     */
    void holdKeyFor(Function<Options, KeyMapping> keyMappingGetter, int ticks);

    /**
     * Holds a key or mouse button for a number of ticks and then releases it.
     *
     * @param key the key or mouse button to hold
     * @param ticks the non-negative number of ticks to hold it
     * @throws IllegalArgumentException if {@code ticks} is negative
     */
    void holdKeyFor(InputConstants.Key key, int ticks);

    /**
     * Holds a GLFW key for a number of ticks and then releases it.
     *
     * @param keyCode the GLFW key code
     * @param ticks the non-negative number of ticks to hold it
     * @throws IllegalArgumentException if {@code ticks} is negative
     */
    void holdKeyFor(int keyCode, int ticks);

    /**
     * Holds a mouse button for a number of ticks and then releases it.
     *
     * @param button the GLFW mouse button
     * @param ticks the non-negative number of ticks to hold it
     * @throws IllegalArgumentException if {@code ticks} is negative
     */
    void holdMouseFor(int button, int ticks);

    /**
     * Sets the connected client's view rotation without advancing a tick.
     *
     * @param yaw the finite yaw in degrees
     * @param pitch the finite pitch in degrees
     * @throws IllegalStateException if no client player is present
     */
    void lookAt(float yaw, float pitch);

    /**
     * Rotates the connected client's eyes toward the center of a block without advancing a tick.
     *
     * @param position the block position to look at
     * @throws IllegalStateException if no client player is present
     */
    void lookAt(BlockPos position);

    /**
     * Sends one Unicode code point through Minecraft's character input handler.
     *
     * @param codePoint the Unicode code point to type
     * @see #typeChars(String)
     */
    void typeChar(int codePoint);

    /**
     * Sends every Unicode code point in a string through Minecraft's character input handler.
     *
     * @param text the text to type
     */
    void typeChars(String text);

    /**
     * Sends a vertical scroll event without advancing a tick.
     *
     * @param vertical the vertical scroll amount
     */
    default void scroll(double vertical) {
        scroll(0.0, vertical);
    }

    /**
     * Sends a two-axis scroll event without advancing a tick.
     *
     * @param horizontal the horizontal scroll amount
     * @param vertical the vertical scroll amount
     */
    void scroll(double horizontal, double vertical);

    /**
     * Sets the cursor position in raw window coordinates without advancing a tick.
     *
     * @param x the raw horizontal coordinate
     * @param y the raw vertical coordinate
     */
    void setCursorPos(double x, double y);

    /**
     * Moves the cursor by an offset in raw window coordinates without advancing a tick.
     *
     * @param deltaX the horizontal offset
     * @param deltaY the vertical offset
     */
    void moveCursor(double deltaX, double deltaY);
}
