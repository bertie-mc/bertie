package io.github.bertie_mc.screenshotcopy;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.imurx.arboard.Clipboard;
import io.github.imurx.arboard.ImageData;

final class ScreenshotClipboard {
    private Clipboard clipboard;

    void copy(NativeImage image) {
        if (clipboard == null) {
            clipboard = new Clipboard();
        }

        byte[] rgba = RgbaPixels.fromAbgr(image.getPixelsRGBA());
        try (ImageData clipboardImage = new ImageData(image.getWidth(), image.getHeight(), rgba)) {
            clipboard.setImage(clipboardImage);
        }
    }
}
