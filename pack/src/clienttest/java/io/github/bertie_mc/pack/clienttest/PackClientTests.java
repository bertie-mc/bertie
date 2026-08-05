package io.github.bertie_mc.pack.clienttest;

import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class PackClientTests {
    private PackClientTests() {}

    @ClientTest
    public static void shaderpackIsStaged(ClientTestContext context) {
        Path shaderpacks = context.computeOnClient(
                client -> client.gameDirectory.toPath().resolve("shaderpacks"));
        try (Stream<Path> contents = Files.list(shaderpacks)) {
            if (contents.noneMatch(path -> Files.isRegularFile(path)
                    && path.getFileName().toString().endsWith(".zip"))) {
                throw new AssertionError("No shaderpack archive was staged in " + shaderpacks);
            }
        } catch (IOException exception) {
            throw new AssertionError("Could not inspect staged shaderpacks in " + shaderpacks, exception);
        }
    }

    @ClientTest
    public static void joinsDedicatedServer(ClientTestContext context) {
        try (var server = context.worldBuilder().createServer()) {
            try (var connection = server.connect()) {
                connection.waitForClientboundPackets();
                connection.waitForServerboundPackets();
                context.runOnClient(client -> {
                    connection.clientPlayer();
                    connection.clientLevel();
                });
            }
        }
    }
}
