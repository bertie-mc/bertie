package io.github.bertie_mc.configmigrations.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MixinTargetsTest {
    @Test
    void commonLoaderScopesRegistrationAndClientCommonLoads()
            throws ClassNotFoundException {
        Set<String> methods = methodNames("net.neoforged.neoforge.internal.CommonModLoader");
        assertHasMixinMethod(methods, "configmigrations$initializeLaunchPolicy");
        assertHasMixinMethod(methods, "configmigrations$runRegistrationPhase");
        assertHasMixinMethod(methods, "configmigrations$runClientOrCommonLoad");
    }

    @Test
    void standardSpecConnectsRegistrationAndAcceptanceToTheActivePhase()
            throws ClassNotFoundException {
        Set<String> methods = methodNames("net.neoforged.neoforge.common.ModConfigSpec");
        assertHasMixinMethod(methods, "configmigrations$registerSpec");
        assertHasMixinMethod(methods, "configmigrations$accept");
        assertNoMixinMethod(methods, "configmigrations$requirePendingMigrations");
        assertNoMixinMethod(methods, "configmigrations$correct");
    }

    @Test
    void icebergSpecConnectsRegistrationAndAcceptanceToTheActivePhase()
            throws ClassNotFoundException {
        Set<String> methods = methodNames(
                "com.anthonyhilyard.iceberg.neoforge.config.NeoForgeIcebergConfigSpec");
        assertHasMixinMethod(methods, "configmigrations$registerSpec");
        assertHasMixinMethod(methods, "configmigrations$accept");
        assertNoMixinMethod(methods, "configmigrations$requirePendingMigrations");
        assertNoMixinMethod(methods, "configmigrations$correct");
    }

    @Test
    void serverLifecycleScopesThePhysicalServerLoad() throws ClassNotFoundException {
        assertHasMixinMethod(
                methodNames("net.neoforged.neoforge.server.ServerLifecycleHooks"),
                "configmigrations$runServerLoad");
    }

    private static Set<String> methodNames(String className) throws ClassNotFoundException {
        Class<?> target = Class.forName(className, false, MixinTargetsTest.class.getClassLoader());
        return Arrays.stream(target.getDeclaredMethods()).map(Method::getName).collect(Collectors.toSet());
    }

    private static void assertHasMixinMethod(Set<String> methods, String fragment) {
        assertTrue(
                methods.stream().anyMatch(method -> method.contains(fragment)),
                () -> "Missing " + fragment + " in " + methods);
    }

    private static void assertNoMixinMethod(Set<String> methods, String fragment) {
        assertFalse(
                methods.stream().anyMatch(method -> method.contains(fragment)),
                () -> "Unexpected " + fragment + " in " + methods);
    }
}
