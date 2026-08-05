package io.github.bertie_mc.testing.client.driver.threading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class TestSchedulerTest {
    @Test
    void dispatchedTaskRethrowsTheOriginalCheckedException() {
        IOException failure = new IOException("callback failed");
        var task = new TestScheduler.DispatchedTask<Object>(
                TestScheduler.TaskTarget.CLIENT,
                () -> {
                    throw failure;
                });

        task.run();

        assertSame(failure, assertThrows(IOException.class, task::await));
    }

    @Test
    void capsCatchUpTicksWithoutInventingATick() {
        assertEquals(0, TestScheduler.capClientTicksPerFrame(0));
        assertEquals(1, TestScheduler.capClientTicksPerFrame(1));
        assertEquals(1, TestScheduler.capClientTicksPerFrame(10));
    }

    @Test
    void recordedFailureRemainsAvailableUntilTheResultBoundaryConsumesIt() {
        TestScheduler.takeFailure();
        try {
            IllegalStateException failure = new IllegalStateException("server crashed");
            TestScheduler.recordFailure(failure);

            assertSame(
                    failure,
                    assertThrows(
                            IllegalStateException.class,
                            TestScheduler::throwRecordedFailure));
            assertSame(failure, TestScheduler.takeFailure());
        } finally {
            TestScheduler.takeFailure();
        }
    }

    @Test
    void terminalFailuresAreKeptSeparateFromPerTestFailures() {
        TestScheduler.takeFailure();
        TestScheduler.takeTerminalFailure();
        try {
            IllegalStateException testFailure = new IllegalStateException("test failed");
            IllegalStateException terminalFailure = new IllegalStateException("report failed");
            TestScheduler.recordFailure(testFailure);
            TestScheduler.recordTerminalFailure(terminalFailure);

            assertSame(testFailure, TestScheduler.takeFailure());
            assertSame(terminalFailure, TestScheduler.takeTerminalFailure());
        } finally {
            TestScheduler.takeFailure();
            TestScheduler.takeTerminalFailure();
        }
    }

    @Test
    void cancellationWakesTheTestThreadBeforeTheMinecraftThreadAcceptsTheTask()
            throws InterruptedException {
        AtomicBoolean actionRan = new AtomicBoolean();
        var task = new TestScheduler.DispatchedTask<>(
                TestScheduler.TaskTarget.CLIENT,
                () -> actionRan.compareAndSet(false, true));
        CountDownLatch waiterStarted = new CountDownLatch(1);
        AtomicReference<Throwable> observedFailure = new AtomicReference<>();
        Thread waiter = Thread.ofPlatform().daemon().start(() -> {
            waiterStarted.countDown();
            try {
                task.await();
            } catch (Throwable failure) {
                observedFailure.set(failure);
            }
        });
        waiterStarted.await();

        IllegalStateException failure = new IllegalStateException("client stopped");
        task.cancel(failure);
        waiter.join(1_000);

        assertFalse(waiter.isAlive(), "the test thread remained blocked on the cancelled task");
        assertSame(failure, observedFailure.get());
        task.run();
        assertFalse(actionRan.get(), "a cancelled task was accepted after its target stopped");
    }

    @Test
    void cancellationWaitsForAnAlreadyRunningTaskToReturn() throws InterruptedException {
        CountDownLatch actionStarted = new CountDownLatch(1);
        CountDownLatch releaseAction = new CountDownLatch(1);
        var task = new TestScheduler.DispatchedTask<>(
                TestScheduler.TaskTarget.CLIENT,
                () -> {
                    actionStarted.countDown();
                    try {
                        releaseAction.await();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }
                    return null;
                });
        AtomicReference<Throwable> observedFailure = new AtomicReference<>();
        CountDownLatch waiterCompleted = new CountDownLatch(1);
        Thread waiter = Thread.ofPlatform().daemon().start(() -> {
            try {
                task.await();
            } catch (Throwable failure) {
                observedFailure.set(failure);
            } finally {
                waiterCompleted.countDown();
            }
        });
        Thread runner = Thread.ofPlatform().daemon().start(task::run);
        assertTrue(actionStarted.await(1, TimeUnit.SECONDS));

        IllegalStateException failure = new IllegalStateException("client stopped");
        task.cancel(failure);
        assertFalse(
                waiterCompleted.await(100, TimeUnit.MILLISECONDS),
                "cancellation released the test thread while its action was still running");

        releaseAction.countDown();
        assertTrue(waiterCompleted.await(1, TimeUnit.SECONDS));
        runner.join(1_000);
        waiter.join(1_000);
        assertSame(failure, observedFailure.get());
    }
}
