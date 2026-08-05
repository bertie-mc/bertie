/**
 * APIs for deterministic Minecraft client integration tests.
 *
 * <p>Declare each test as a {@code public static void} method annotated with
 * {@link io.github.bertie_mc.testing.client.ClientTest ClientTest} and accepting one
 * {@link io.github.bertie_mc.testing.client.context.ClientTestContext ClientTestContext}.
 * Bertie discovers the methods from NeoForge's mod scan data and runs them sequentially on a
 * dedicated client-test thread. After each method, the context closes its remaining resources and
 * returns the client to the title screen.
 *
 * <h2>Threads and ticks</h2>
 *
 * <p>Test methods do not run on Minecraft's render or logical-server threads. Use
 * {@link io.github.bertie_mc.testing.client.context.ClientTestContext#runOnClient
 * ClientTestContext.runOnClient} for client state and
 * {@link io.github.bertie_mc.testing.client.context.ServerContext#runOnServer
 * ServerContext.runOnServer} for server state. Exceptions thrown by either callback are returned
 * to the client-test thread.
 *
 * <p>During a test, the game advances only when a wait helper runs a tick. Consequently, actions
 * such as changing a key state may not take effect until the test waits. While a logical server is
 * running, Bertie runs one logical-server tick for each client tick and limits the client to one
 * tick per rendered frame.
 *
 * <p>Network delivery is not guaranteed to finish in an arbitrary number of ticks. Prefer
 * {@link io.github.bertie_mc.testing.client.context.ServerConnection#waitForClientboundPackets()
 * ServerConnection.waitForClientboundPackets} and
 * {@link io.github.bertie_mc.testing.client.context.ServerConnection#waitForServerboundPackets()
 * ServerConnection.waitForServerboundPackets} after the relevant packet has been sent.
 *
 * <h2>Owned resources</h2>
 *
 * <p>Use integrated worlds, dedicated servers, and dedicated-server connections in
 * try-with-resources statements. Closing an integrated world or dedicated connection returns the
 * client to the title screen. Closing a dedicated server stops its in-process server and closes
 * its remaining connections.
 *
 * <h2>Deterministic defaults</h2>
 *
 * <p>Bertie disables the tutorial, clouds, the accessibility onboarding screen, pausing on lost
 * focus, and music, and sets the render distance to five chunks. Consistent test worlds are flat,
 * use seed {@code 1}, disable structures, daylight, weather, and mob spawning, and set the spawn
 * radius to zero. In-process dedicated servers additionally disable online mode and spawn
 * protection, accept one player, disable the watchdog, and use asynchronous chunk writes except
 * on Windows. Tests may change these settings, and
 * {@link io.github.bertie_mc.testing.client.context.ClientTestContext#restoreDefaultGameOptions()
 * restoreDefaultGameOptions} restores the client-test option baseline.
 */
package io.github.bertie_mc.testing.client;
