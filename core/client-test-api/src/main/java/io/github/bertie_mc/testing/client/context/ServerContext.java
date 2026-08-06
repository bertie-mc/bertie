package io.github.bertie_mc.testing.client.context;

import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;

/**
 * Operations against a logical server used by a client test.
 *
 * <p>Unless otherwise specified, methods may only be called from the client-test thread.
 */
public interface ServerContext {
    /**
     * Runs a command as the server on the logical-server thread.
     *
     * @param command the command without a leading slash
     */
    void runCommand(String command);

    /**
     * Runs an action on the logical-server thread and waits for it to complete. If called from that
     * server thread, the action runs directly.
     *
     * @param action the action to run
     * @param <E> the checked exception thrown by the action
     * @throws E if the action throws it
     */
    <E extends Throwable> void runOnServer(FailableConsumer<MinecraftServer, E> action) throws E;

    /**
     * Computes a value on the logical-server thread and waits for it. If called from that server
     * thread, the function runs directly.
     *
     * @param function the function to run
     * @param <T> the returned value type
     * @param <E> the checked exception thrown by the function
     * @return the function result
     * @throws E if the function throws it
     */
    <T, E extends Throwable> T computeOnServer(FailableFunction<MinecraftServer, T, E> function) throws E;

    /**
     * Waits for a server predicate using {@link ClientTestContext#DEFAULT_TIMEOUT_TICKS}.
     *
     * @param condition the predicate to evaluate on the logical-server thread
     * @return the number of ticks waited, possibly zero
     * @throws AssertionError if the predicate is still false at the timeout
     */
    int waitFor(Predicate<MinecraftServer> condition);

    /**
     * Waits for a server predicate.
     *
     * @param condition the predicate to evaluate on the logical-server thread
     * @param timeoutTicks a positive timeout in ticks, or {@link ClientTestContext#NO_TIMEOUT}
     * @return the number of ticks waited, possibly zero
     * @throws IllegalArgumentException if the timeout is neither positive nor
     *     {@link ClientTestContext#NO_TIMEOUT}
     * @throws AssertionError if the predicate is still false at the timeout
     */
    int waitFor(Predicate<MinecraftServer> condition, int timeoutTicks);

    /**
     * Waits for a described server condition using
     * {@link ClientTestContext#DEFAULT_TIMEOUT_TICKS}.
     *
     * @param description a human-readable condition used in timeout failures
     * @param condition the predicate to evaluate on the logical-server thread
     * @return the number of ticks waited, possibly zero
     * @throws AssertionError if the predicate is still false at the timeout
     */
    int waitFor(String description, Predicate<MinecraftServer> condition);

    /**
     * Waits for a described server condition.
     *
     * @param description a human-readable condition used in timeout failures
     * @param condition the predicate to evaluate on the logical-server thread
     * @param timeoutTicks a positive timeout in ticks, or {@link ClientTestContext#NO_TIMEOUT}
     * @return the number of ticks waited, possibly zero
     * @throws IllegalArgumentException if the timeout is neither positive nor
     *     {@link ClientTestContext#NO_TIMEOUT}
     * @throws AssertionError if the predicate is still false at the timeout
     */
    int waitFor(String description, Predicate<MinecraftServer> condition, int timeoutTicks);
}
