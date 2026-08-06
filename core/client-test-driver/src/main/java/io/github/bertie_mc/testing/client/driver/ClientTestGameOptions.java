package io.github.bertie_mc.testing.client.driver;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.bertie_mc.testing.client.driver.mixin.options.OptionsAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.sounds.SoundSource;

/** Owns the initialized game-option baseline for the client-test process. */
public final class ClientTestGameOptions {
    private static final int RENDER_DISTANCE = 5;

    private static final GameOptionsBaseline DEFAULTS = new GameOptionsBaseline();

    private ClientTestGameOptions() {}

    public static void initialize(Options options) {
        options.tutorialStep = TutorialSteps.NONE;
        options.cloudStatus().set(CloudStatus.OFF);
        options.onboardAccessibility = false;
        options.pauseOnLostFocus = false;
        options.renderDistance().set(RENDER_DISTANCE);
        options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0);

        captureBaseline(options);
    }

    static void captureBaseline(Options options) {
        DEFAULTS.capture(access -> ((OptionsAccessor) options).bertie$processOptions(access));
    }

    public static void restore(Options options) {
        DEFAULTS.restore(access -> ((OptionsAccessor) options).bertie$processOptions(access));
    }

    static final class GameOptionsBaseline {
        private Map<String, String> values = Map.of();

        void capture(Consumer<Options.FieldAccess> processor) {
            Map<String, String> captured = new HashMap<>();
            processor.accept(new CapturingFieldAccess(captured));
            values = Map.copyOf(captured);
        }

        void restore(Consumer<Options.FieldAccess> processor) {
            processor.accept(new RestoringFieldAccess(values));
        }
    }

    private record CapturingFieldAccess(Map<String, String> values) implements Options.FieldAccess {
        @Override
        public int process(String name, int value) {
            values.put(name, Integer.toString(value));
            return value;
        }

        @Override
        public boolean process(String name, boolean value) {
            values.put(name, Boolean.toString(value));
            return value;
        }

        @Override
        public String process(String name, String value) {
            values.put(name, value);
            return value;
        }

        @Override
        public float process(String name, float value) {
            values.put(name, Float.toString(value));
            return value;
        }

        @Override
        public <T> T process(String name, T value, Function<String, T> decoder, Function<T, String> encoder) {
            values.put(name, encoder.apply(value));
            return value;
        }

        @Override
        public <T> void process(String name, OptionInstance<T> option) {
            String encoded = option.codec()
                    .encodeStart(JsonOps.INSTANCE, option.get())
                    .getOrThrow(message -> new IllegalStateException("Cannot capture option " + name + ": " + message))
                    .toString();
            values.put(name, encoded);
        }
    }

    private record RestoringFieldAccess(Map<String, String> values) implements Options.FieldAccess {
        @Override
        public int process(String name, int value) {
            String encoded = values.get(name);
            return encoded == null ? value : Integer.parseInt(encoded);
        }

        @Override
        public boolean process(String name, boolean value) {
            String encoded = values.get(name);
            return encoded == null ? value : Boolean.parseBoolean(encoded);
        }

        @Override
        public String process(String name, String value) {
            return values.getOrDefault(name, value);
        }

        @Override
        public float process(String name, float value) {
            String encoded = values.get(name);
            return encoded == null ? value : Float.parseFloat(encoded);
        }

        @Override
        public <T> T process(String name, T value, Function<String, T> decoder, Function<T, String> encoder) {
            String encoded = values.get(name);
            return encoded == null ? value : decoder.apply(encoded);
        }

        @Override
        public <T> void process(String name, OptionInstance<T> option) {
            String encoded = values.get(name);
            if (encoded != null) {
                T restored = option.codec()
                        .parse(JsonOps.INSTANCE, JsonParser.parseString(encoded))
                        .getOrThrow(
                                message -> new IllegalStateException("Cannot restore option " + name + ": " + message));
                option.set(restored);
            }
        }
    }
}
