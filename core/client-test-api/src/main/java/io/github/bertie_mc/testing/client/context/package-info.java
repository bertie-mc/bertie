/**
 * Client, server, and connection contexts used during a client test.
 *
 * <p>Unless a method says otherwise, call context operations from the dedicated client-test
 * thread. Access Minecraft client state through
 * {@link io.github.bertie_mc.testing.client.context.ClientTestContext#runOnClient} and logical
 * server state through
 * {@link io.github.bertie_mc.testing.client.context.ServerContext#runOnServer}.
 *
 * <p>{@link io.github.bertie_mc.testing.client.context.IntegratedWorldContext},
 * {@link io.github.bertie_mc.testing.client.context.DedicatedServerContext}, and
 * {@link io.github.bertie_mc.testing.client.context.DedicatedServerConnection} own resources and
 * are intended for try-with-resources statements.
 */
package io.github.bertie_mc.testing.client.context;
