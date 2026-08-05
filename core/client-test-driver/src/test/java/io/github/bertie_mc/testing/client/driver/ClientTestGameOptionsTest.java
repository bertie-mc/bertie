package io.github.bertie_mc.testing.client.driver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

final class ClientTestGameOptionsTest {
    @Test
    void newerCaptureReplacesTheConstructorBaselineAndIncludesModOptions() {
        var baseline = new ClientTestGameOptions.GameOptionsBaseline();
        Function<String, String> identity = Function.identity();

        baseline.capture(access -> {
            access.process("integer", 5);
            access.process("boolean", false);
            access.process("string", "constructor");
            access.process("float", 0.5F);
            access.process("generic", "constructor", identity, identity);
        });
        baseline.capture(access -> {
            access.process("integer", 8);
            access.process("boolean", true);
            access.process("string", "ready tick");
            access.process("float", 0.75F);
            access.process("generic", "ready tick", identity, identity);
            access.process("mod option", "registered", identity, identity);
        });

        baseline.restore(access -> {
            assertEquals(8, access.process("integer", 12));
            assertEquals(true, access.process("boolean", false));
            assertEquals("ready tick", access.process("string", "changed"));
            assertEquals(0.75F, access.process("float", 1.0F));
            assertEquals(
                    "ready tick",
                    access.process("generic", "changed", identity, identity));
            assertEquals(
                    "registered",
                    access.process("mod option", "changed", identity, identity));
            assertEquals(
                    "preserved",
                    access.process("registered later", "preserved", identity, identity));
        });
    }

    @Test
    void encodedMutableOptionsAreIndependentAcrossRestores() {
        var baseline = new ClientTestGameOptions.GameOptionsBaseline();
        var original = new ArrayList<>(List.of("vanilla", "mod"));
        baseline.capture(access -> access.process(
                "resourcePacks",
                original,
                ClientTestGameOptionsTest::decodeList,
                ClientTestGameOptionsTest::encodeList));

        original.add("mutated-after-capture");
        List<String> firstRestore = restoreList(baseline);
        assertEquals(List.of("vanilla", "mod"), firstRestore);
        assertNotSame(original, firstRestore);

        firstRestore.add("mutated-after-restore");
        List<String> secondRestore = restoreList(baseline);
        assertEquals(List.of("vanilla", "mod"), secondRestore);
        assertNotSame(firstRestore, secondRestore);
    }

    private static List<String> restoreList(
            ClientTestGameOptions.GameOptionsBaseline baseline) {
        var restored = new AtomicReference<List<String>>();
        baseline.restore(access -> restored.set(access.process(
                "resourcePacks",
                List.of("current"),
                ClientTestGameOptionsTest::decodeList,
                ClientTestGameOptionsTest::encodeList)));
        return restored.get();
    }

    private static String encodeList(List<String> values) {
        return String.join("\n", values);
    }

    private static List<String> decodeList(String value) {
        return new ArrayList<>(List.of(value.split("\n", -1)));
    }
}
