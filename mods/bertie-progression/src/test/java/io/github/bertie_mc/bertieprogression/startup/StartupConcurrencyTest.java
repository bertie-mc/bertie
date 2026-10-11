package io.github.bertie_mc.bertieprogression.startup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

class StartupConcurrencyTest {
    private Class<?> transformedRegistration() throws Exception {
        ClassNode node = new ClassNode();
        try (var input = ConfigRegistrationFixture.class.getResourceAsStream("ConfigRegistrationFixture.class")) {
            new ClassReader(input).accept(node, 0);
        }
        new StartupMixinPlugin()
                .preApply("eu.midnightdust.lib.config.MidnightConfig", node, "MidnightConfigMixin", null);
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        byte[] bytes = writer.toByteArray();
        return new ClassLoader(getClass().getClassLoader()) {
            Class<?> define() {
                return defineClass(null, bytes, 0, bytes.length);
            }
        }.define();
    }

    @Test
    void registrationCannotMutateEntriesWhileAnotherRegistrationReadsThem() throws Exception {
        Class<?> registration = transformedRegistration();
        Method init = registration.getMethod("init", String.class, Class.class);
        CountDownLatch reading =
                (CountDownLatch) registration.getField("READING").get(null);
        CountDownLatch proceed =
                (CountDownLatch) registration.getField("CONTINUE").get(null);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread reader = new Thread(() -> invoke(init, "reader", failure));
        Thread writer = new Thread(() -> invoke(init, "writer", failure));
        reader.start();
        try {
            assertTrue(reading.await(2, TimeUnit.SECONDS));
            writer.start();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (writer.isAlive() && writer.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }
            assertEquals(Thread.State.BLOCKED, writer.getState());
        } finally {
            proceed.countDown();
            reader.join(3000);
            writer.join(3000);
        }
        assertEquals(null, failure.get());
        Map<?, ?> entries = (Map<?, ?>) registration.getField("ENTRIES").get(null);
        assertEquals(java.util.List.of("reader", "writer"), new ArrayList<>(entries.keySet()));
    }

    @Test
    void aFailedRegistrationReleasesTheClassMonitor() throws Exception {
        Class<?> registration = transformedRegistration();
        Method init = registration.getMethod("init", String.class, Class.class);
        InvocationTargetException failure =
                assertThrows(InvocationTargetException.class, () -> init.invoke(null, "throw", Object.class));
        assertInstanceOf(IllegalStateException.class, failure.getCause());
        AtomicReference<Throwable> nextFailure = new AtomicReference<>();
        Thread next = new Thread(() -> invoke(init, "writer", nextFailure));
        next.start();
        next.join(2000);
        assertTrue(!next.isAlive());
        assertEquals(null, nextFailure.get());
    }

    private static void invoke(Method init, String id, AtomicReference<Throwable> failure) {
        try {
            init.invoke(null, id, Object.class);
        } catch (Throwable exception) {
            failure.compareAndSet(null, exception);
        }
    }
}
