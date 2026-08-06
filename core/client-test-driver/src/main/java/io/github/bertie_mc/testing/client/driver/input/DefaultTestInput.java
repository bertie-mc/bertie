package io.github.bertie_mc.testing.client.driver.input;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.bertie_mc.testing.client.TestInput;
import io.github.bertie_mc.testing.client.driver.context.DefaultClientTestContext;
import io.github.bertie_mc.testing.client.driver.mixin.input.KeyboardHandlerInvoker;
import io.github.bertie_mc.testing.client.driver.mixin.input.MouseHandlerInvoker;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Delivers simulated keyboard and mouse events through Minecraft's normal handlers. */
public final class DefaultTestInput implements TestInput, AutoCloseable {
    private static final Set<InputConstants.Key> SIMULATED_KEYS_DOWN = ConcurrentHashMap.newKeySet();

    private final DefaultClientTestContext context;
    private final Set<InputConstants.Key> heldKeys = new HashSet<>();
    private final double initialMouseX;
    private final double initialMouseY;
    private boolean closed;

    public DefaultTestInput(DefaultClientTestContext context) {
        this.context = context;
        this.initialMouseX = context.computeOnClient(client -> client.mouseHandler.xpos());
        this.initialMouseY = context.computeOnClient(client -> client.mouseHandler.ypos());
    }

    public static boolean isKeyDown(int keyCode) {
        return SIMULATED_KEYS_DOWN.contains(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
    }

    @Override
    public synchronized void holdKey(KeyMapping keyMapping) {
        holdKey(boundKey(keyMapping, "hold"));
    }

    @Override
    public synchronized void holdKey(Function<Options, KeyMapping> keyMappingGetter) {
        Objects.requireNonNull(keyMappingGetter);
        KeyMapping keyMapping = context.computeOnClient(client -> keyMappingGetter.apply(client.options));
        holdKey(keyMapping);
    }

    @Override
    public synchronized void holdKey(InputConstants.Key key) {
        ensureOpen();
        Objects.requireNonNull(key);
        if (!heldKeys.add(key)) {
            return;
        }
        context.runOnClient(client -> {
            SIMULATED_KEYS_DOWN.add(key);
            pressOrRelease(client, key, GLFW.GLFW_PRESS);
        });
    }

    @Override
    public synchronized void holdKey(int keyCode) {
        holdKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
    }

    @Override
    public synchronized void holdMouse(int button) {
        holdKey(InputConstants.Type.MOUSE.getOrCreate(button));
    }

    @Override
    public synchronized void holdControl() {
        holdKey(Util.getPlatform() == Util.OS.OSX ? GLFW.GLFW_KEY_LEFT_SUPER : InputConstants.KEY_LCONTROL);
    }

    @Override
    public synchronized void holdShift() {
        holdKey(InputConstants.KEY_LSHIFT);
    }

    @Override
    public synchronized void holdAlt() {
        holdKey(InputConstants.KEY_LALT);
    }

    @Override
    public synchronized void releaseKey(KeyMapping keyMapping) {
        releaseKey(boundKey(keyMapping, "release"));
    }

    @Override
    public synchronized void releaseKey(Function<Options, KeyMapping> keyMappingGetter) {
        Objects.requireNonNull(keyMappingGetter);
        KeyMapping keyMapping = context.computeOnClient(client -> keyMappingGetter.apply(client.options));
        releaseKey(keyMapping);
    }

    @Override
    public synchronized void releaseKey(InputConstants.Key key) {
        ensureOpen();
        Objects.requireNonNull(key);
        if (!heldKeys.remove(key)) {
            return;
        }
        context.runOnClient(client -> {
            SIMULATED_KEYS_DOWN.remove(key);
            pressOrRelease(client, key, GLFW.GLFW_RELEASE);
        });
    }

    @Override
    public synchronized void releaseKey(int keyCode) {
        releaseKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
    }

    @Override
    public synchronized void releaseMouse(int button) {
        releaseKey(InputConstants.Type.MOUSE.getOrCreate(button));
    }

    @Override
    public synchronized void releaseControl() {
        releaseKey(Util.getPlatform() == Util.OS.OSX ? GLFW.GLFW_KEY_LEFT_SUPER : InputConstants.KEY_LCONTROL);
    }

    @Override
    public synchronized void releaseShift() {
        releaseKey(InputConstants.KEY_LSHIFT);
    }

    @Override
    public synchronized void releaseAlt() {
        releaseKey(InputConstants.KEY_LALT);
    }

    @Override
    public synchronized void pressKey(KeyMapping keyMapping) {
        pressKey(boundKey(keyMapping, "press"));
    }

    @Override
    public synchronized void pressKey(Function<Options, KeyMapping> keyMappingGetter) {
        Objects.requireNonNull(keyMappingGetter);
        KeyMapping keyMapping = context.computeOnClient(client -> keyMappingGetter.apply(client.options));
        pressKey(keyMapping);
    }

    @Override
    public synchronized void pressKey(InputConstants.Key key) {
        ensureOpen();
        Objects.requireNonNull(key);
        holdKey(key);
        releaseKey(key);
        context.waitTick();
    }

    @Override
    public synchronized void pressKey(int keyCode) {
        pressKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
    }

    @Override
    public synchronized void pressMouse(int button) {
        pressKey(InputConstants.Type.MOUSE.getOrCreate(button));
    }

    @Override
    public synchronized void holdKeyFor(KeyMapping keyMapping, int ticks) {
        holdKeyFor(boundKey(keyMapping, "hold"), ticks);
    }

    @Override
    public synchronized void holdKeyFor(Function<Options, KeyMapping> keyMappingGetter, int ticks) {
        Objects.requireNonNull(keyMappingGetter);
        KeyMapping keyMapping = context.computeOnClient(client -> keyMappingGetter.apply(client.options));
        holdKeyFor(keyMapping, ticks);
    }

    @Override
    public synchronized void holdKeyFor(InputConstants.Key key, int ticks) {
        ensureOpen();
        Objects.requireNonNull(key);
        if (ticks < 0) {
            throw new IllegalArgumentException("ticks cannot be negative");
        }
        holdKey(key);
        context.waitTicks(ticks);
        releaseKey(key);
    }

    @Override
    public synchronized void holdKeyFor(int keyCode, int ticks) {
        holdKeyFor(InputConstants.Type.KEYSYM.getOrCreate(keyCode), ticks);
    }

    @Override
    public synchronized void holdMouseFor(int button, int ticks) {
        holdKeyFor(InputConstants.Type.MOUSE.getOrCreate(button), ticks);
    }

    @Override
    public synchronized void lookAt(float yaw, float pitch) {
        ensureOpen();
        if (!Float.isFinite(yaw)) {
            throw new IllegalArgumentException("yaw must be finite");
        }
        if (!Float.isFinite(pitch)) {
            throw new IllegalArgumentException("pitch must be finite");
        }
        context.runOnClient(client -> {
            if (client.player == null) {
                throw new IllegalStateException("A client player must be present to look around");
            }
            client.player.setYRot(yaw);
            client.player.setXRot(pitch);
        });
    }

    @Override
    public synchronized void lookAt(BlockPos position) {
        ensureOpen();
        Objects.requireNonNull(position);
        context.runOnClient(client -> {
            if (client.player == null) {
                throw new IllegalStateException("A client player must be present to look around");
            }
            client.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(position));
        });
    }

    @Override
    public synchronized void typeChar(int codePoint) {
        ensureOpen();
        context.runOnClient(client -> typeChar(client, codePoint));
    }

    @Override
    public synchronized void typeChars(String text) {
        ensureOpen();
        Objects.requireNonNull(text);
        context.runOnClient(client -> {
            text.codePoints().forEach(codePoint -> typeChar(client, codePoint));
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
        context.runOnClient(client ->
                moveCursorTo(client, client.mouseHandler.xpos() + deltaX, client.mouseHandler.ypos() + deltaY));
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
            case KEYSYM -> client.keyboardHandler.keyPress(window, key.getValue(), 0, action, modifiers);
            case SCANCODE ->
                client.keyboardHandler.keyPress(window, GLFW.GLFW_KEY_UNKNOWN, key.getValue(), action, modifiers);
            case MOUSE ->
                ((MouseHandlerInvoker) client.mouseHandler).bertie$onPress(window, key.getValue(), action, modifiers);
        }
    }

    private static void typeChar(Minecraft client, int codePoint) {
        long window = client.getWindow().getWindow();
        ((KeyboardHandlerInvoker) client.keyboardHandler).bertie$charTyped(window, codePoint, modifiers(window));
    }

    private static InputConstants.Key boundKey(KeyMapping keyMapping, String action) {
        Objects.requireNonNull(keyMapping);
        if (keyMapping.isUnbound()) {
            throw new AssertionError(
                    "Cannot " + action + " key mapping " + keyMapping.getName() + " because it is unbound");
        }
        return keyMapping.getKey();
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
