package io.github.bertie_mc.testing.client;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;

/** Operations against the logical server owned by a client test. */
public interface ServerContext {
    void runCommand(String command);

    void runOnServer(Consumer<MinecraftServer> action);

    <T> T computeOnServer(Function<MinecraftServer, T> action);

    void waitFor(String description, Predicate<MinecraftServer> condition);

    void waitFor(
            String description, Predicate<MinecraftServer> condition, int timeoutTicks);
}
