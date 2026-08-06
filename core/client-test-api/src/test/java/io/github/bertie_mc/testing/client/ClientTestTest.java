package io.github.bertie_mc.testing.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.bertie_mc.testing.client.context.ClientTestContext;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

final class ClientTestTest {
    @Test
    void annotationMarksTheCompleteMethodDeclaration() throws ReflectiveOperationException {
        Method method = Example.class.getDeclaredMethod("example", ClientTestContext.class);
        ClientTest annotation = method.getAnnotation(ClientTest.class);

        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isStatic(method.getModifiers()));
        assertEquals(void.class, method.getReturnType());
        assertNotNull(annotation);
    }

    private static final class Example {
        @ClientTest
        public static void example(ClientTestContext context) {}
    }
}
