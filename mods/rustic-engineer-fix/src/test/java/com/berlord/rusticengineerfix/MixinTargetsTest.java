package com.berlord.rusticengineerfix;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinTargetsTest {

    @Test
    void airshipProcedureReceivesYawCaptureAndRestoreHooks() throws ClassNotFoundException {
        Set<String> methods = methodNames("net.mcreator.rusticengineer.procedures.AirshipFlyingProcedure");
        assertHasMixinMethod(methods, "rusticengineerfix$captureYaw");
        assertHasMixinMethod(methods, "rusticengineerfix$restoreOldYaw");
    }

    @Test
    void dragonflyProcedureReceivesPitchAndVerticalHooks() throws ClassNotFoundException {
        Set<String> methods = methodNames("net.mcreator.rusticengineer.procedures.DragonflyFlyingProcedure");
        assertHasMixinMethod(methods, "rusticengineerfix$levelForwardFlight");
        assertHasMixinMethod(methods, "rusticengineerfix$combineVertical");
    }

    private static Set<String> methodNames(String className) throws ClassNotFoundException {
        return Arrays.stream(Class.forName(className).getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
    }

    private static void assertHasMixinMethod(Set<String> methods, String prefix) {
        assertTrue(methods.stream().anyMatch(name -> name.contains(prefix)),
                () -> "Missing " + prefix + " in " + methods);
    }
}
