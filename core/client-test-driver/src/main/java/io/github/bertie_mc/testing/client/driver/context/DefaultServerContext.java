package io.github.bertie_mc.testing.client.driver.context;

import io.github.bertie_mc.testing.client.context.ClientTestContext;
import io.github.bertie_mc.testing.client.context.ServerContext;
import io.github.bertie_mc.testing.client.driver.threading.TestScheduler;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;

/** Dispatches commands, callbacks, and state waits to one logical server. */
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
        runOnServer(
                current -> current.getCommands().performPrefixedCommand(current.createCommandSourceStack(), command));
    }

    @Override
    public <E extends Throwable> void runOnServer(FailableConsumer<MinecraftServer, E> action) throws E {
        Objects.requireNonNull(action);
        TestScheduler.runOnServer(server, () -> action.accept(server));
    }

    @Override
    public <T, E extends Throwable> T computeOnServer(FailableFunction<MinecraftServer, T, E> action) throws E {
        Objects.requireNonNull(action);
        return TestScheduler.computeOnServer(server, () -> action.apply(server));
    }

    @Override
    public int waitFor(Predicate<MinecraftServer> condition) {
        return waitFor("the server condition", condition);
    }

    @Override
    public int waitFor(Predicate<MinecraftServer> condition, int timeoutTicks) {
        return waitFor("the server condition", condition, timeoutTicks);
    }

    @Override
    public int waitFor(String description, Predicate<MinecraftServer> condition) {
        return waitFor(description, condition, ClientTestContext.DEFAULT_TIMEOUT_TICKS);
    }

    @Override
    public int waitFor(String description, Predicate<MinecraftServer> condition, int timeoutTicks) {
        Objects.requireNonNull(condition);
        return context.waitForCondition(
                description, () -> computeOnServer(server -> condition.test(server)), timeoutTicks);
    }
}
