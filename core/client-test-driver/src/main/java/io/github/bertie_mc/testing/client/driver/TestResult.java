package io.github.bertie_mc.testing.client.driver;

import java.time.Duration;

/** The name, duration, and optional failure recorded for one discovered test method. */
record TestResult(String name, Duration duration, Throwable failure) {
    static TestResult passed(String name, Duration duration) {
        return new TestResult(name, duration, null);
    }

    static TestResult failed(String name, Duration duration, Throwable failure) {
        return new TestResult(name, duration, failure);
    }

    boolean failed() {
        return failure != null;
    }
}
