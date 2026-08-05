package io.github.bertie_mc.testing.client.context;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class ServerConnectionTest {
    @Test
    void chunkWaitsAllowOneMinuteByDefault() {
        assertEquals(1_200, ServerConnection.DEFAULT_CHUNK_TIMEOUT_TICKS);
    }
}
