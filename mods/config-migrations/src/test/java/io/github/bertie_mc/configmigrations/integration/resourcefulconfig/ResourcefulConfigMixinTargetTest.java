package io.github.bertie_mc.configmigrations.integration.resourcefulconfig;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ResourcefulConfigMixinTargetTest {
    @Test
    void bothConfiguratorRegistrationPathsWrapTheirInitialSave() {
        Set<String> methods = Arrays.stream(Configurator.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertTrue(methods.stream().anyMatch(name -> name.contains("configmigrations$migrateParsedClass")));
        assertTrue(methods.stream().anyMatch(name -> name.contains("configmigrations$migrateRegisteredConfig")));
    }
}
