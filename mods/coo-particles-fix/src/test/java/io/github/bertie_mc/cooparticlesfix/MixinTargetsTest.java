package io.github.bertie_mc.cooparticlesfix;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MixinTargetsTest {
    @Test
    void cooEventBusReceivesDedicatedServerListenerGuard() throws ClassNotFoundException {
        Class<?> eventBus = Class.forName("cn.coostack.cooparticlesapi.event.CooEventBus");
        assertTrue(Arrays.stream(eventBus.getDeclaredMethods())
                .map(Method::getName)
                .anyMatch(name -> name.contains("cooparticlesfix$skipClientOnlyTestListener")));
    }
}
