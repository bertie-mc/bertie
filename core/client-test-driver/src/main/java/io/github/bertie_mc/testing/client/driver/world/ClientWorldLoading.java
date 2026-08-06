package io.github.bertie_mc.testing.client.driver.world;

import io.github.bertie_mc.testing.client.driver.context.DefaultClientTestContext;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Handles Minecraft's world-confirmation screens and complete client-side world loading. */
public final class ClientWorldLoading {
    private static final String EXPERIMENTAL_WORLD_TITLE = "selectWorld.warning.experimental.title";

    private ClientWorldLoading() {}

    static <T> T awaitCreation(DefaultClientTestContext context, String description, CompletionStage<T> completion) {
        Objects.requireNonNull(completion);
        var future = completion.toCompletableFuture();
        context.waitForInfrastructure(description, () -> {
            if (future.isDone()) {
                return true;
            }
            handleConfirmation(context);
            return future.isDone();
        });
        return context.awaitInfrastructure(description, completion);
    }

    public static void waitForWorld(
            DefaultClientTestContext context, String description, Predicate<Minecraft> readiness) {
        Objects.requireNonNull(readiness);
        context.waitForInfrastructure(description, () -> {
            if (handleConfirmation(context)) {
                return false;
            }
            return context.computeOnClient(client -> {
                String terminalFailure = terminalFailureFor(client.screen);
                if (terminalFailure != null) {
                    throw new AssertionError(description + " failed: " + terminalFailure);
                }
                return readiness.test(client)
                        && !(client.screen instanceof LevelLoadingScreen)
                        && !(client.screen instanceof ReceivingLevelScreen);
            });
        });
    }

    private static boolean handleConfirmation(DefaultClientTestContext context) {
        Confirmation confirmation = context.computeOnClient(client -> confirmationFor(client.screen));
        return switch (confirmation) {
            case EXPERIMENTAL_WORLD -> {
                context.clickScreenButton("gui.yes");
                yield true;
            }
            case JOIN_WITHOUT_BACKUP -> {
                context.clickScreenButton("selectWorld.backupJoinSkipButton");
                yield true;
            }
            case NONE -> false;
        };
    }

    static Confirmation confirmationFor(Screen screen) {
        if (screen instanceof BackupConfirmScreen) {
            return Confirmation.JOIN_WITHOUT_BACKUP;
        }
        if (screen instanceof ConfirmScreen
                && screen.getTitle().getContents() instanceof TranslatableContents title
                && EXPERIMENTAL_WORLD_TITLE.equals(title.getKey())) {
            return Confirmation.EXPERIMENTAL_WORLD;
        }
        return Confirmation.NONE;
    }

    static String terminalFailureFor(Screen screen) {
        return screen instanceof DisconnectedScreen
                ? screen.getNarrationMessage().getString()
                : null;
    }

    enum Confirmation {
        NONE,
        EXPERIMENTAL_WORLD,
        JOIN_WITHOUT_BACKUP
    }
}
