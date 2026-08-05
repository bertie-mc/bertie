package io.github.bertie_mc.testing.client.driver.screenshot;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/** Captures screenshots only after Minecraft has rendered the requested frame. */
public final class ClientTestScreenshots {
    private static final AtomicReference<Request> PENDING = new AtomicReference<>();

    private ClientTestScreenshots() {}

    public static CompletionStage<Path> afterNextFrame(Path target) {
        Request request = new Request(Objects.requireNonNull(target));
        if (!PENDING.compareAndSet(null, request)) {
            throw new IllegalStateException("Another client-test screenshot is already pending");
        }
        return request.completion;
    }

    /** Called from the render thread immediately after the main game renderer completes. */
    public static void afterRender(Minecraft client) {
        Request request = PENDING.getAndSet(null);
        if (request == null) {
            return;
        }

        try {
            Files.createDirectories(request.target.getParent());
            try (NativeImage image = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
                image.writeToFile(request.target);
            }
            request.completion.complete(request.target);
        } catch (IOException exception) {
            request.completion.completeExceptionally(
                    new IllegalStateException("Cannot write screenshot " + request.target, exception));
        } catch (Throwable failure) {
            request.completion.completeExceptionally(failure);
        }
    }

    private static final class Request {
        private final Path target;
        private final CompletableFuture<Path> completion = new CompletableFuture<>();

        private Request(Path target) {
            this.target = target;
        }
    }
}
