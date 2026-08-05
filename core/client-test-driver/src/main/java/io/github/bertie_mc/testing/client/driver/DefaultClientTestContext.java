package io.github.bertie_mc.testing.client.driver;

import io.github.bertie_mc.testing.client.ClientTestContext;
import io.github.bertie_mc.testing.client.TestInput;
import io.github.bertie_mc.testing.client.TestWorldBuilder;
import io.github.bertie_mc.testing.client.driver.mixin.CycleButtonAccessor;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

final class DefaultClientTestContext implements ClientTestContext, AutoCloseable {
    private static final int NO_TIMEOUT = -1;

    private final Minecraft client;
    private final Path diagnostics;
    private final DefaultTestInput input;
    private final ClientTestResources resources = new ClientTestResources();

    DefaultClientTestContext(Minecraft client, Path diagnostics) {
        this.client = Objects.requireNonNull(client);
        this.diagnostics = Objects.requireNonNull(diagnostics);
        this.input = new DefaultTestInput(this);
    }

    @Override
    public TestInput input() {
        return input;
    }

    @Override
    public void waitTick() {
        TestScheduler.runTick();
    }

    @Override
    public void waitTicks(int ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("ticks cannot be negative");
        }
        for (int tick = 0; tick < ticks; tick++) {
            waitTick();
        }
    }

    @Override
    public void waitFor(String description, Predicate<Minecraft> condition) {
        waitFor(description, condition, DEFAULT_TIMEOUT_TICKS);
    }

    @Override
    public void waitFor(
            String description, Predicate<Minecraft> condition, int timeoutTicks) {
        Objects.requireNonNull(condition);
        waitForCondition(
                description,
                () -> computeOnClient(client -> condition.test(client)),
                timeoutTicks);
    }

    void waitForCondition(String description, BooleanSupplier condition) {
        waitForCondition(description, condition, DEFAULT_TIMEOUT_TICKS);
    }

    void waitForCondition(
            String description, BooleanSupplier condition, int timeoutTicks) {
        Objects.requireNonNull(description);
        Objects.requireNonNull(condition);
        if (timeoutTicks == NO_TIMEOUT) {
            while (!condition.getAsBoolean()) {
                TestScheduler.runTick();
            }
            return;
        }
        if (timeoutTicks <= 0) {
            throw new IllegalArgumentException("timeoutTicks must be positive");
        }
        for (int waited = 0; waited < timeoutTicks; waited++) {
            if (condition.getAsBoolean()) {
                return;
            }
            TestScheduler.runTick();
        }
        if (!condition.getAsBoolean()) {
            throw new AssertionError(
                    "Timed out after " + timeoutTicks + " ticks waiting for " + description);
        }
    }

    void waitForInfrastructure(String description, BooleanSupplier condition) {
        // The task runner owns the process deadline; loading a real pack is not a test assertion.
        waitForCondition(description, condition, NO_TIMEOUT);
    }

    @Override
    public void waitForScreen(Class<? extends Screen> screenType) {
        waitForScreen(screenType, DEFAULT_TIMEOUT_TICKS);
    }

    @Override
    public void waitForScreen(Class<? extends Screen> screenType, int timeoutTicks) {
        Objects.requireNonNull(screenType);
        waitFor(
                screenType.getSimpleName(),
                current -> screenType.isInstance(current.screen),
                timeoutTicks);
    }

    @Override
    public void setScreen(Supplier<? extends Screen> screen) {
        Objects.requireNonNull(screen);
        runOnClient(current -> current.setScreen(screen.get()));
    }

    @Override
    public void clickScreenButton(String translationKey) {
        Objects.requireNonNull(translationKey);
        if (tryClickScreenButton(translationKey)) {
            return;
        }
        String screenName = computeOnClient(current -> current.screen == null
                ? "null"
                : current.screen.getClass().getName());
        throw new AssertionError(
                "No button for translation key " + translationKey + " on " + screenName);
    }

    @Override
    public boolean tryClickScreenButton(String translationKey) {
        Objects.requireNonNull(translationKey);
        String expected = Component.translatable(translationKey).getString();
        return computeOnClient(current -> tryPressButton(current.screen, expected));
    }

    @Override
    public void runOnClient(Consumer<Minecraft> action) {
        Objects.requireNonNull(action);
        TestScheduler.runOnClient(() -> action.accept(client));
    }

    @Override
    public <T> T computeOnClient(Function<Minecraft, T> action) {
        Objects.requireNonNull(action);
        return TestScheduler.computeOnClient(() -> action.apply(client));
    }

    @Override
    public TestWorldBuilder worldBuilder() {
        return new DefaultTestWorldBuilder(this);
    }

    @Override
    public void restoreDefaultGameOptions() {
        runOnClient(current -> ClientTestGameOptions.restore(current.options));
    }

    @Override
    public Path takeScreenshot(String name) {
        String filename = name.endsWith(".png") ? name : name + ".png";
        Path target = diagnostics.resolve(filename).toAbsolutePath().normalize();
        if (!target.startsWith(diagnostics.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Screenshot name escapes the diagnostics directory");
        }
        CompletionStage<Path> screenshot = computeOnClient(
                current -> ClientTestScreenshots.afterNextFrame(target));
        return awaitInfrastructure("the screenshot frame", screenshot);
    }

    <T> T await(String description, CompletionStage<T> stage) {
        return await(description, stage, DEFAULT_TIMEOUT_TICKS);
    }

    <T> T await(String description, CompletionStage<T> stage, int timeoutTicks) {
        CompletableFuture<T> future = Objects.requireNonNull(stage).toCompletableFuture();
        waitForCondition(description, future::isDone, timeoutTicks);
        return completed(future);
    }

    <T> T awaitInfrastructure(String description, CompletionStage<T> stage) {
        CompletableFuture<T> future = Objects.requireNonNull(stage).toCompletableFuture();
        waitForInfrastructure(description, future::isDone);
        return completed(future);
    }

    <T extends AutoCloseable> T own(T resource) {
        return resources.own(resource);
    }

    @Override
    public void close() {
        Throwable failure = null;
        try {
            input.close();
        } catch (Throwable closeFailure) {
            failure = ClientTestResources.append(failure, closeFailure);
        }
        try {
            resources.close();
        } catch (Throwable closeFailure) {
            failure = ClientTestResources.append(failure, closeFailure);
        }
        try {
            disconnectToTitle();
        } catch (Throwable closeFailure) {
            failure = ClientTestResources.append(failure, closeFailure);
        }
        ClientTestResources.rethrow(failure);
    }

    void disconnectToTitle() {
        runOnClient(current -> {
            if (current.level != null
                    || current.getConnection() != null
                    || current.hasSingleplayerServer()) {
                if (current.level != null) {
                    current.level.disconnect();
                }
                current.disconnect(new TitleScreen());
            } else {
                current.setScreen(new TitleScreen());
            }
        });
        waitForInfrastructure(
                "the client to return to the title screen",
                () -> computeOnClient(current -> current.level == null
                        && current.getConnection() == null
                        && !current.hasSingleplayerServer()));
        runOnClient(current -> {
            if (!(current.screen instanceof TitleScreen)) {
                current.setScreen(new TitleScreen());
            }
        });
    }

    void requireClientThread() {
        if (!client.isSameThread()) {
            throw new IllegalStateException(
                    "Client state must be accessed through ClientTestContext.runOnClient");
        }
    }

    private static boolean tryPressButton(Screen screen, String expected) {
        if (screen == null) {
            return false;
        }
        for (var renderable : screen.renderables) {
            if (renderable instanceof AbstractButton button
                    && pressMatchingButton(button, expected)) {
                return true;
            }
            if (renderable instanceof LayoutElement layout) {
                AtomicBoolean pressed = new AtomicBoolean();
                layout.visitWidgets(widget -> {
                    if (!pressed.get()
                            && widget instanceof AbstractButton button
                            && pressMatchingButton(button, expected)) {
                        pressed.set(true);
                    }
                });
                if (pressed.get()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean pressMatchingButton(AbstractButton button, String expected) {
        String label = button instanceof CycleButton<?>
                ? ((CycleButtonAccessor) button).bertie$getName().getString()
                : button.getMessage().getString();
        if (!expected.equals(label)) {
            return false;
        }
        button.onPress();
        return true;
    }

    private static <T> T completed(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw exception;
        }
    }
}
