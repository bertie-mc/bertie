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
        assertZipIsStaged(context, "shaderpacks");
    }

    @ClientTest
    public static void datapackIsStaged(ClientTestContext context) {
        assertZipIsStaged(context, "datapacks");
    }

    private static void assertZipIsStaged(ClientTestContext context, String directory) {
        Path packs =
                context.computeOnClient(client -> client.gameDirectory.toPath().resolve(directory));
        try (Stream<Path> contents = Files.list(packs)) {
            if (contents.noneMatch(path ->
                    Files.isRegularFile(path) && path.getFileName().toString().endsWith(".zip"))) {
                throw new AssertionError("No pack archive was staged in " + packs);
            }
        } catch (IOException exception) {
            throw new AssertionError("Could not inspect staged packs in " + packs, exception);
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
