package io.github.bertie_mc.testing.client.driver;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.bertie_mc.testing.client.TestInput;
import io.github.bertie_mc.testing.client.driver.mixin.KeyboardHandlerInvoker;
import io.github.bertie_mc.testing.client.driver.mixin.MouseHandlerInvoker;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class DefaultTestInput implements TestInput, AutoCloseable {
    private static final Set<InputConstants.Key> SIMULATED_KEYS_DOWN =
            ConcurrentHashMap.newKeySet();

    private final DefaultClientTestContext context;
    private final Set<InputConstants.Key> heldKeys = new HashSet<>();
    private final double initialMouseX;
    private final double initialMouseY;
    private boolean closed;

    DefaultTestInput(DefaultClientTestContext context) {
        this.context = context;
        this.initialMouseX = context.computeOnClient(client -> client.mouseHandler.xpos());
        this.initialMouseY = context.computeOnClient(client -> client.mouseHandler.ypos());
    }

    public static boolean isKeyDown(int keyCode) {
        return SIMULATED_KEYS_DOWN.contains(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
    }

    @Override
    public synchronized void holdKey(InputConstants.Key key) {
        ensureOpen();
        if (!heldKeys.add(key)) {
            return;
        }
        context.runOnClient(client -> {
            SIMULATED_KEYS_DOWN.add(key);
            pressOrRelease(client, key, GLFW.GLFW_PRESS);
        });
    }

    @Override
    public synchronized void releaseKey(InputConstants.Key key) {
        ensureOpen();
        if (!heldKeys.remove(key)) {
            return;
        }
        context.runOnClient(client -> {
            SIMULATED_KEYS_DOWN.remove(key);
            pressOrRelease(client, key, GLFW.GLFW_RELEASE);
        });
    }

    @Override
    public synchronized void pressKey(InputConstants.Key key) {
        ensureOpen();
        holdKey(key);
        releaseKey(key);
        context.waitTick();
    }

    @Override
    public synchronized void holdKeyFor(InputConstants.Key key, int ticks) {
        ensureOpen();
        if (ticks < 0) {
            throw new IllegalArgumentException("ticks cannot be negative");
        }
        holdKey(key);
        context.waitTicks(ticks);
        releaseKey(key);
    }

    @Override
    public synchronized void typeChars(String text) {
        ensureOpen();
        context.runOnClient(client -> {
            long window = client.getWindow().getWindow();
            int modifiers = modifiers(window);
            text.codePoints().forEach(codePoint ->
                    ((KeyboardHandlerInvoker) client.keyboardHandler)
                            .bertie$charTyped(window, codePoint, modifiers));
        });
    }

    @Override
    public synchronized void scroll(double horizontal, double vertical) {
        ensureOpen();
        context.runOnClient(client -> ((MouseHandlerInvoker) client.mouseHandler)
                .bertie$onScroll(client.getWindow().getWindow(), horizontal, vertical));
    }

    @Override
    public synchronized void setCursorPos(double x, double y) {
        ensureOpen();
        context.runOnClient(client -> moveCursorTo(client, x, y));
    }

    @Override
    public synchronized void moveCursor(double deltaX, double deltaY) {
        ensureOpen();
        context.runOnClient(client -> moveCursorTo(
                client,
                client.mouseHandler.xpos() + deltaX,
                client.mouseHandler.ypos() + deltaY));
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        Set<InputConstants.Key> keys = Set.copyOf(heldKeys);
        heldKeys.clear();
        try {
            context.runOnClient(client -> {
                for (InputConstants.Key key : keys) {
                    SIMULATED_KEYS_DOWN.remove(key);
                    pressOrRelease(client, key, GLFW.GLFW_RELEASE);
                }
                moveCursorTo(client, initialMouseX, initialMouseY);
            });
        } finally {
            SIMULATED_KEYS_DOWN.removeAll(keys);
        }
    }

    private static void pressOrRelease(Minecraft client, InputConstants.Key key, int action) {
        long window = client.getWindow().getWindow();
        int modifiers = modifiers(window);
        switch (key.getType()) {
            case KEYSYM -> client.keyboardHandler.keyPress(
                    window, key.getValue(), 0, action, modifiers);
            case SCANCODE -> client.keyboardHandler.keyPress(
                    window, GLFW.GLFW_KEY_UNKNOWN, key.getValue(), action, modifiers);
            case MOUSE -> ((MouseHandlerInvoker) client.mouseHandler)
                    .bertie$onPress(window, key.getValue(), action, modifiers);
        }
    }

    private static void moveCursorTo(Minecraft client, double x, double y) {
        ((MouseHandlerInvoker) client.mouseHandler)
                .bertie$onMove(client.getWindow().getWindow(), x, y);
    }

    private static int modifiers(long window) {
        int modifiers = 0;
        if (keyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            modifiers |= GLFW.GLFW_MOD_SHIFT;
        }
        if (keyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            modifiers |= GLFW.GLFW_MOD_CONTROL;
        }
        if (keyDown(window, GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT)) {
            modifiers |= GLFW.GLFW_MOD_ALT;
        }
        if (keyDown(window, GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER)) {
            modifiers |= GLFW.GLFW_MOD_SUPER;
        }
        return modifiers;
    }

    private static boolean keyDown(long window, int left, int right) {
        return InputConstants.isKeyDown(window, left) || InputConstants.isKeyDown(window, right);
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("The client-test input is already closed");
        }
    }
}
