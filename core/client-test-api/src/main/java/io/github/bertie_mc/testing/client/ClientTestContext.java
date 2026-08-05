package io.github.bertie_mc.testing.client;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** State-oriented operations available to a {@link ClientTest}. */
public interface ClientTestContext {
    int DEFAULT_TIMEOUT_TICKS = 200;

    TestInput input();

    void waitTick();

    void waitTicks(int ticks);

    void waitFor(String description, Predicate<Minecraft> condition);

    void waitFor(String description, Predicate<Minecraft> condition, int timeoutTicks);

    void waitForScreen(Class<? extends Screen> screenType);

    void waitForScreen(Class<? extends Screen> screenType, int timeoutTicks);

    void setScreen(Supplier<? extends Screen> screen);

    void clickScreenButton(String translationKey);

    boolean tryClickScreenButton(String translationKey);

    /** Runs an action on the render thread and waits for it to complete. */
    void runOnClient(Consumer<Minecraft> action);

    /** Computes a value on the render thread and waits for it to complete. */
    <T> T computeOnClient(Function<Minecraft, T> action);

    TestWorldBuilder worldBuilder();

    void restoreDefaultGameOptions();

    Path takeScreenshot(String name);
}
