package io.github.bertie_mc.testing.client;

import com.mojang.blaze3d.platform.InputConstants;

/** Input delivered through Minecraft's keyboard and mouse handlers. */
public interface TestInput {
    /** Holds a key until {@link #releaseKey(InputConstants.Key)} is called. */
    void holdKey(InputConstants.Key key);

    default void holdKey(int keyCode) {
        holdKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
    }

    void releaseKey(InputConstants.Key key);

    default void releaseKey(int keyCode) {
        releaseKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
    }

    /** Presses and releases a key, then waits one game tick. */
    void pressKey(InputConstants.Key key);

    default void pressKey(int keyCode) {
        pressKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
    }

    /** Holds a key for the requested number of game ticks, then releases it. */
    void holdKeyFor(InputConstants.Key key, int ticks);

    default void holdKeyFor(int keyCode, int ticks) {
        holdKeyFor(InputConstants.Type.KEYSYM.getOrCreate(keyCode), ticks);
    }

    default void holdMouse(int button) {
        holdKey(InputConstants.Type.MOUSE.getOrCreate(button));
    }

    default void releaseMouse(int button) {
        releaseKey(InputConstants.Type.MOUSE.getOrCreate(button));
    }

    default void pressMouse(int button) {
        pressKey(InputConstants.Type.MOUSE.getOrCreate(button));
    }

    default void holdMouseFor(int button, int ticks) {
        holdKeyFor(InputConstants.Type.MOUSE.getOrCreate(button), ticks);
    }

    void typeChars(String text);

    default void scroll(double vertical) {
        scroll(0.0, vertical);
    }

    void scroll(double horizontal, double vertical);

    /** Sets the cursor position in raw window coordinates. */
    void setCursorPos(double x, double y);

    /** Moves the cursor by an offset in raw window coordinates. */
    void moveCursor(double deltaX, double deltaY);
}
