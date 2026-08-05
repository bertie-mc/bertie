package io.github.bertie_mc.testing.client.driver;

import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import io.github.bertie_mc.testing.client.driver.context.DefaultClientTestContext;
import io.github.bertie_mc.testing.client.driver.input.ClientTestKeyDisplayNames;
import io.github.bertie_mc.testing.client.driver.network.ClientTestNetwork;
import io.github.bertie_mc.testing.client.driver.threading.TestScheduler;
import java.lang.annotation.ElementType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

/** Discovers and runs {@link ClientTest} methods inside a normal Minecraft client. */
@Mod(value = ClientTestDriver.MOD_ID, dist = Dist.CLIENT)
public final class ClientTestDriver {
    public static final String MOD_ID = "bertie_client_test_driver";
    static final String RESULTS_PROPERTY = "bertie.clienttest.report";
    static final String DIAGNOSTICS_PROPERTY = "bertie.clienttest.diagnostics";

    private static final Type CLIENT_TEST_TYPE = Type.getType(ClientTest.class);

    private final AtomicBoolean armed = new AtomicBoolean();
    private final AtomicBoolean started = new AtomicBoolean();

    public ClientTestDriver(IEventBus modBus) {
        modBus.addListener(this::onLoadComplete);
        modBus.addListener(ClientTestNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> armed.set(true));
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (isStartupReady(armed.get(), client.getOverlay() != null)
                && started.compareAndSet(false, true)) {
            TestScheduler.start(client, () -> runTests(client));
        }
    }

    static boolean isStartupReady(boolean armed, boolean overlayPresent) {
        return armed && !overlayPresent;
    }

    private void runTests(Minecraft client) {
        Path results = configuredPath(RESULTS_PROPERTY, "clienttest-results.xml");
        List<TestResult> outcomes = collectReportedOutcomes(
                () -> initializeRuntime(client),
                collected -> runDiscoveredTests(
                        client,
                        configuredPath(DIAGNOSTICS_PROPERTY, "clienttest-diagnostics"),
                        collected));
        ClientTestResults.write(results, outcomes);
    }

    private static void initializeRuntime(Minecraft client) {
        TestScheduler.runOnClient(() -> {
            ClientTestKeyDisplayNames.preload(client.options);
            ClientTestGameOptions.captureBaseline(client.options);
        });
    }

    private void runDiscoveredTests(
            Minecraft client, Path diagnostics, List<TestResult> outcomes) {
        List<TestMethod> tests = discoverTests();
        if (tests.isEmpty()) {
            throw new IllegalStateException("No @ClientTest methods were discovered");
        }
        for (TestMethod test : tests) {
            outcomes.add(execute(test, client, diagnostics));
        }
    }

    static List<TestResult> collectReportedOutcomes(
            Runnable initialization, Consumer<List<TestResult>> suite) {
        List<TestResult> outcomes = new ArrayList<>();
        try {
            initialization.run();
            suite.accept(outcomes);
        } catch (Throwable failure) {
            outcomes.add(TestResult.failed("client-test driver", Duration.ZERO, failure));
        }
        return List.copyOf(outcomes);
    }

    private TestResult execute(TestMethod test, Minecraft client, Path diagnostics) {
        long startedAt = System.nanoTime();
        DefaultClientTestContext context = null;
        Throwable failure = TestScheduler.takeFailure();
        try {
            TestScheduler.rethrowFailure(failure);
            context = new DefaultClientTestContext(client, diagnostics);
            context.restoreDefaultGameOptions();
            invoke(test.method(), context);
            failure = ClientTestResources.append(failure, TestScheduler.takeFailure());
        } catch (Throwable exception) {
            failure = ClientTestResources.append(failure, exception);
            failure = ClientTestResources.append(failure, TestScheduler.takeFailure());
        }

        if (failure != null && context != null) {
            try {
                context.takeScreenshot("failure-" + sanitize(test.name()));
            } catch (Throwable screenshotFailure) {
                failure = ClientTestResources.append(failure, screenshotFailure);
            }
        }
        failure = ClientTestResources.append(failure, TestScheduler.takeFailure());
        if (context != null) {
            try {
                context.close();
            } catch (Throwable cleanupFailure) {
                failure = ClientTestResources.append(failure, cleanupFailure);
            }
        }
        failure = ClientTestResources.append(failure, TestScheduler.takeFailure());

        Duration duration = Duration.ofNanos(System.nanoTime() - startedAt);
        return failure == null
                ? TestResult.passed(test.name(), duration)
                : TestResult.failed(test.name(), duration, failure);
    }

    private static void invoke(Method method, ClientTestContext context) throws Exception {
        try {
            method.invoke(null, context);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) {
                throw checked;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        }
    }

    private static List<TestMethod> discoverTests() {
        List<TestMethod> methods = new ArrayList<>();
        ModList.get().getAllScanData().stream()
                .map(ModFileScanData::getAnnotations)
                .flatMap(annotations -> annotations.stream())
                .filter(annotation -> annotation.targetType() == ElementType.METHOD)
                .filter(annotation -> CLIENT_TEST_TYPE.equals(annotation.annotationType()))
                .forEach(annotation -> collectMethods(
                        annotation.clazz().getClassName(), annotation.memberName(), methods));
        methods.sort(Comparator.comparing(TestMethod::name));
        return List.copyOf(methods);
    }

    private static void collectMethods(String className, String memberName, List<TestMethod> target) {
        try {
            Class<?> declaringClass = Class.forName(
                    className, true, ClientTestDriver.class.getClassLoader());
            for (Method method : declaringClass.getDeclaredMethods()) {
                ClientTest annotation = method.getAnnotation(ClientTest.class);
                if (annotation == null || !matchesScannedMethod(method, memberName)) {
                    continue;
                }
                validate(method);
                target.add(new TestMethod(
                        declaringClass.getName() + "." + method.getName(), method));
            }
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Cannot load client test class " + className, exception);
        }
    }

    static boolean matchesScannedMethod(Method method, String memberName) {
        return (method.getName() + Type.getMethodDescriptor(method)).equals(memberName);
    }

    private static void validate(Method method) {
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers)
                || !Modifier.isStatic(modifiers)
                || method.getReturnType() != void.class
                || method.getParameterCount() != 1
                || method.getParameterTypes()[0] != ClientTestContext.class) {
            throw new IllegalStateException(
                    "@ClientTest method must be public static void and accept one ClientTestContext: "
                            + method.toGenericString());
        }
    }

    private static Path configuredPath(String property, String fallback) {
        return Path.of(System.getProperty(property, fallback)).toAbsolutePath().normalize();
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]+", "-");
    }

    private record TestMethod(String name, Method method) {}
}
