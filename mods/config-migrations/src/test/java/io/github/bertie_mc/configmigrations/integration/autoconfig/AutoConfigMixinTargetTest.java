package io.github.bertie_mc.configmigrations.integration.autoconfig;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import me.shedaniel.autoconfig.AutoConfig;
import org.junit.jupiter.api.Test;

class AutoConfigMixinTargetTest {
    @Test
    void registrationDecoratesTheSerializerFactoryResult() {
        assertTrue(Arrays.stream(AutoConfig.class.getDeclaredMethods())
                .map(Method::getName)
                .anyMatch(name -> name.contains("configmigrations$wrapSerializer")));
    }
}
