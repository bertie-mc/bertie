package io.github.bertie_mc.testing.client.driver;

import io.github.bertie_mc.testing.client.ClientTestContext;
import io.github.bertie_mc.testing.client.ServerContext;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;

class DefaultServerContext implements ServerContext {
    protected final DefaultClientTestContext context;
    protected final MinecraftServer server;

    DefaultServerContext(DefaultClientTestContext context, MinecraftServer server) {
        this.context = Objects.requireNonNull(context);
        this.server = Objects.requireNonNull(server);
    }

    @Override
    public void runCommand(String command) {
        Objects.requireNonNull(command);
        runOnServer(current -> current.getCommands()
                .performPrefixedCommand(current.createCommandSourceStack(), command));
    }

    @Override
    public void runOnServer(Consumer<MinecraftServer> action) {
        Objects.requireNonNull(action);
        TestScheduler.runOnServer(server, () -> action.accept(server));
    }

    @Override
    public <T> T computeOnServer(Function<MinecraftServer, T> action) {
        Objects.requireNonNull(action);
        return TestScheduler.computeOnServer(server, () -> action.apply(server));
    }

    @Override
    public void waitFor(String description, Predicate<MinecraftServer> condition) {
        waitFor(description, condition, ClientTestContext.DEFAULT_TIMEOUT_TICKS);
    }

    @Override
    public void waitFor(
            String description, Predicate<MinecraftServer> condition, int timeoutTicks) {
        Objects.requireNonNull(condition);
        context.waitForCondition(
                description,
                () -> computeOnServer(server -> condition.test(server)),
                timeoutTicks);
    }
}
