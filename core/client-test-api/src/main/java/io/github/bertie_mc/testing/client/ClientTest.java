package io.github.bertie_mc.testing.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a Minecraft client integration test.
 *
 * <p>The annotated method must be {@code public static void} and accept exactly one
 * {@link io.github.bertie_mc.testing.client.context.ClientTestContext}. Bertie discovers these
 * methods from NeoForge's mod scan data and runs them sequentially.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ClientTest {}
