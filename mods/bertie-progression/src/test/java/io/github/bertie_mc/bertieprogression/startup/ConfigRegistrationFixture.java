package io.github.bertie_mc.bertieprogression.startup;

import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Shared-map registration used to exercise the transformed method under contention. */
public final class ConfigRegistrationFixture {
    public static final LinkedHashMap<String, Class<?>> ENTRIES = new LinkedHashMap<>();
    public static final CountDownLatch READING = new CountDownLatch(1);
    public static final CountDownLatch CONTINUE = new CountDownLatch(1);

    public static void init(String id, Class<?> config) throws InterruptedException {
        ENTRIES.put(id, config);
        if (id.equals("throw")) {
            throw new IllegalStateException("invalid config");
        }
        if (id.equals("reader")) {
            var entries = ENTRIES.values().iterator();
            READING.countDown();
            if (!CONTINUE.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test registration timed out");
            }
            while (entries.hasNext()) {
                entries.next();
            }
        }
    }
}
