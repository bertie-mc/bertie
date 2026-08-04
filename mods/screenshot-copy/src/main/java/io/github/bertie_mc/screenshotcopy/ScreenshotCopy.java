package io.github.bertie_mc.screenshotcopy;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenshotEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(value = ScreenshotCopy.MOD_ID, dist = Dist.CLIENT)
public final class ScreenshotCopy {
    public static final String MOD_ID = "screenshotcopy";
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ScreenshotClipboard clipboard = new ScreenshotClipboard();

    public ScreenshotCopy() {
        NeoForge.EVENT_BUS.addListener(this::onScreenshot);
    }

    private void onScreenshot(ScreenshotEvent event) {
        try {
            clipboard.copy(event.getImage());
        } catch (RuntimeException exception) {
            LOGGER.error("Could not copy screenshot to the native clipboard", exception);
            event.setResultMessage(Component.translatable(
                    "message.screenshotcopy.copy_failed", exception.toString()));
        }
    }
}
