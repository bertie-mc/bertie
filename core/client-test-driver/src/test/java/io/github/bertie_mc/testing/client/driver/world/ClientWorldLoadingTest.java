package io.github.bertie_mc.testing.client.driver.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

final class ClientWorldLoadingTest {
    @Test
    void recognizesOnlyTheWorldConfirmationsOwnedByTheTestDriver() {
        var experimental = new ConfirmScreen(
                ignored -> {}, Component.translatable("selectWorld.warning.experimental.title"), Component.empty());
        var unrelated = new ConfirmScreen(ignored -> {}, Component.translatable("disconnect.lost"), Component.empty());
        var backup = new BackupConfirmScreen(
                () -> {}, (proceed, eraseCache) -> {}, Component.empty(), Component.empty(), false);

        assertEquals(
                ClientWorldLoading.Confirmation.EXPERIMENTAL_WORLD, ClientWorldLoading.confirmationFor(experimental));
        assertEquals(ClientWorldLoading.Confirmation.JOIN_WITHOUT_BACKUP, ClientWorldLoading.confirmationFor(backup));
        assertEquals(ClientWorldLoading.Confirmation.NONE, ClientWorldLoading.confirmationFor(unrelated));
        assertEquals(ClientWorldLoading.Confirmation.NONE, ClientWorldLoading.confirmationFor(null));
    }

    @Test
    void reportsAConnectionFailureInsteadOfWaitingForever() {
        var screen = new DisconnectedScreen(
                null, Component.literal("Connection failed"), Component.literal("Connection refused"));

        String failure = ClientWorldLoading.terminalFailureFor(screen);

        assertTrue(failure.contains("Connection refused"));
        assertNull(ClientWorldLoading.terminalFailureFor(null));
    }
}
