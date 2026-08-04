package io.github.bertie_mc.screenshotcopy.test;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.ClientTestContext;
import io.github.imurx.arboard.Clipboard;
import io.github.imurx.arboard.ImageData;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;

public final class ScreenshotCopyClientTests {
    private static final String SCREENSHOT_NAME = "bertie-screenshot-copy-clienttest.png";
    private static final byte[] SENTINEL_PIXEL = {0x13, 0x37, 0x42, (byte) 0xFF};

    private ScreenshotCopyClientTests() {
    }

    @ClientTest
    public static void copiesScreenshotThroughNeoForgeEvent(ClientTestContext context) {
        Path screenshot = context.computeOnClient(client -> client.gameDirectory.toPath()
                .resolve(Screenshot.SCREENSHOT_DIR)
                .resolve(SCREENSHOT_NAME));
        try {
            Files.deleteIfExists(screenshot);
        } catch (IOException exception) {
            throw new AssertionError("Could not clear the previous client-test screenshot", exception);
        }

        try (Clipboard clipboard = new Clipboard(); ImageData sentinel = new ImageData(1, 1, SENTINEL_PIXEL)) {
            clipboard.setImage(sentinel);
        }

        CompletableFuture<Component> saved = new CompletableFuture<>();
        int[] expectedSize = context.computeOnClient(client -> {
            var target = client.getMainRenderTarget();
            Screenshot.grab(
                    client.gameDirectory,
                    SCREENSHOT_NAME,
                    target,
                    saved::complete);
            return new int[] {target.width, target.height};
        });

        context.waitFor("screenshot save", client -> saved.isDone());
        if (!Files.isRegularFile(screenshot)) {
            throw new AssertionError("Minecraft did not save the test screenshot at " + screenshot);
        }

        try (InputStream file = Files.newInputStream(screenshot);
                NativeImage savedImage = NativeImage.read(file);
                Clipboard clipboard = new Clipboard();
                ImageData clipboardImage = clipboard.getImage()) {
            if (savedImage.getWidth() != expectedSize[0] || savedImage.getHeight() != expectedSize[1]) {
                throw new AssertionError(
                        "Saved screenshot size was " + savedImage.getWidth() + "x" + savedImage.getHeight()
                                + "; expected " + expectedSize[0] + "x" + expectedSize[1]);
            }

            byte[] expectedPixels = rgbaBytes(savedImage.getPixelsRGBA());
            byte[] actualPixels = clipboardImage.getImage();
            if (!Arrays.equals(actualPixels, expectedPixels)) {
                throw new AssertionError("Clipboard pixels do not match the screenshot saved by Minecraft");
            }
        } catch (IOException exception) {
            throw new AssertionError("Could not read the screenshot saved by Minecraft", exception);
        }
    }

    private static byte[] rgbaBytes(int[] pixels) {
        ByteBuffer bytes = ByteBuffer.allocate(pixels.length * Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        bytes.asIntBuffer().put(pixels);
        return bytes.array();
    }
}
