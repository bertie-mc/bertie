package io.github.bertie_mc.testing.client.driver.threading;

import io.github.bertie_mc.testing.client.driver.ClientTestResources;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.function.FailableSupplier;

/** Coordinates the test, client, and current logical-server threads. */
public final class TestScheduler {
    private static final int PHASE_TICK = 0;
    private static final int PHASE_TEST = 1;
    private static final int PHASE_MASK = 1;

    private static final Phaser PHASER = new Phaser();
    private static final Semaphore CLIENT_SEMAPHORE = new Semaphore(0);
    private static final Semaphore SERVER_SEMAPHORE = new Semaphore(0);
    private static final AtomicBoolean ACTIVE = new AtomicBoolean();
    private static final AtomicBoolean CLIENT_REGISTERED = new AtomicBoolean();
    private static final AtomicBoolean CLIENT_STOP_REQUESTED = new AtomicBoolean();
    private static final AtomicBoolean SERVER_REGISTERED = new AtomicBoolean();
    private static final AtomicReference<Throwable> FAILURE = new AtomicReference<>();
    private static final AtomicReference<DispatchedTask<?>> PENDING_TASK = new AtomicReference<>();

    private static volatile Thread testThread;
    private static volatile Minecraft client;
    private static volatile MinecraftServer server;
    private static volatile boolean clientCanAcceptTasks;
    private static volatile boolean serverCanAcceptTasks;
    private static volatile CountDownLatch testThreadTermination;
    private static Throwable terminalFailure;

    private TestScheduler() {}

    public static void start(Minecraft minecraft, Runnable testRunner) {
        Objects.requireNonNull(minecraft);
        Objects.requireNonNull(testRunner);
        if (!ACTIVE.compareAndSet(false, true)) {
            throw new IllegalStateException("The client-test scheduler is already running");
        }

        client = minecraft;
        CLIENT_STOP_REQUESTED.set(false);
        if (!CLIENT_REGISTERED.compareAndSet(false, true)) {
            ACTIVE.set(false);
            throw new IllegalStateException(
                    "The Minecraft client is already registered with the client-test scheduler");
        }
        PHASER.bulkRegister(2);
        CountDownLatch termination = new CountDownLatch(1);
        testThreadTermination = termination;
        Thread thread = Thread.ofPlatform().name("bertie-client-tests").unstarted(() -> {
            testThread = Thread.currentThread();
            enterPhase(PHASE_TEST);
            try {
                testRunner.run();
            } catch (Throwable failure) {
                recordTerminalFailure(failure);
            } finally {
                try {
                    stopClientAndReleaseThreads();
                } catch (Throwable failure) {
                    recordTerminalFailure(failure);
                } finally {
                    termination.countDown();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /** Called at the start of each actual Minecraft client tick. */
    public static void clientTick() {
        if (!ACTIVE.get() || !CLIENT_REGISTERED.get()) {
            return;
        }
        clientCanAcceptTasks = true;
        enterPhase(PHASE_TEST);
        acceptTasks(TaskTarget.CLIENT);
        enterPhase(PHASE_TICK);
    }

    /** Called at the start of each actual logical-server tick. */
    public static void serverTick(MinecraftServer tickingServer) {
        if (!ACTIVE.get() || tickingServer != server || !SERVER_REGISTERED.get()) {
            return;
        }
        serverCanAcceptTasks = true;
        enterPhase(PHASE_TEST);
        acceptTasks(TaskTarget.SERVER);
        enterPhase(PHASE_TICK);
    }

    public static void serverThreadStarted(MinecraftServer startedServer) {
        if (!ACTIVE.get()) {
            return;
        }
        if (!SERVER_REGISTERED.compareAndSet(false, true)) {
            throw new IllegalStateException("A logical server is already registered with the client-test scheduler");
        }
        server = startedServer;
        PHASER.register();
    }

    public static void serverThreadStopped(MinecraftServer stoppedServer) {
        if (server != stoppedServer || !SERVER_REGISTERED.compareAndSet(true, false)) {
            return;
        }
        serverCanAcceptTasks = false;
        server = null;
        PHASER.arriveAndDeregister();
        cancelPendingTask(
                TaskTarget.SERVER, new IllegalStateException("The logical server stopped before running a test task"));
    }

    public static boolean canAcceptServerTasks(MinecraftServer target) {
        return target == server && SERVER_REGISTERED.get() && serverCanAcceptTasks;
    }

    public static void requireNoServerRunning() {
        checkTestThread("create a test world");
        if (SERVER_REGISTERED.get()) {
            throw new IllegalStateException("Cannot create a test world while another logical server is running");
        }
    }

    public static void recordFailure(Throwable failure) {
        FAILURE.compareAndSet(null, Objects.requireNonNull(failure));
    }

    /** Called before Minecraft handles a fatal client failure. */
    public static void clientFailed(Throwable failure) {
        if (!CLIENT_REGISTERED.get()) {
            return;
        }
        Throwable actual =
                Objects.requireNonNullElseGet(failure, () -> new IllegalStateException("The Minecraft client failed"));
        recordFailure(actual);
        clientTerminated(actual);
    }

    /** Called when the Minecraft render loop returns, normally or exceptionally. */
    public static void clientStopped() {
        IllegalStateException unexpectedStop =
                new IllegalStateException("The Minecraft client stopped before the client-test suite completed");
        if (CLIENT_REGISTERED.get() && ACTIVE.get() && !CLIENT_STOP_REQUESTED.get()) {
            recordFailure(unexpectedStop);
        }
        clientTerminated(unexpectedStop);
    }

    public static Throwable takeFailure() {
        return FAILURE.getAndSet(null);
    }

    public static synchronized Throwable takeTerminalFailure() {
        Throwable failure = terminalFailure;
        terminalFailure = null;
        return failure;
    }

    public static void awaitTestThreadTermination() {
        CountDownLatch termination = testThreadTermination;
        if (termination == null) {
            return;
        }
        boolean interrupted = false;
        while (true) {
            try {
                termination.await();
                break;
            } catch (InterruptedException exception) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public static synchronized void recordTerminalFailure(Throwable failure) {
        terminalFailure = ClientTestResources.append(terminalFailure, Objects.requireNonNull(failure));
    }

    public static boolean isClientTaskRunning() {
        Minecraft currentClient = client;
        DispatchedTask<?> task = PENDING_TASK.get();
        return currentClient != null
                && currentClient.isSameThread()
                && task != null
                && task.target() == TaskTarget.CLIENT;
    }

    public static int capClientTicksPerFrame(int ticksPerFrame) {
        return Math.min(ticksPerFrame, 1);
    }

    public static void runTick() {
        checkTestThread("waitTick");
        if (clientCanAcceptTasks) {
            CLIENT_SEMAPHORE.release();
        }
        if (serverCanAcceptTasks) {
            SERVER_SEMAPHORE.release();
        }
        enterPhase(PHASE_TEST);
        throwRecordedFailure();
    }

    public static void throwRecordedFailure() {
        Throwable failure = FAILURE.get();
        if (failure != null) {
            rethrowFailure(failure);
        }
    }

    public static <E extends Throwable> void runOnClient(FailableRunnable<E> action) throws E {
        computeOnClient(() -> {
            action.run();
            return null;
        });
    }

    public static <T, E extends Throwable> T computeOnClient(FailableSupplier<T, E> action) throws E {
        Objects.requireNonNull(action);
        Minecraft currentClient = Objects.requireNonNull(client, "The client is not running");
        if (currentClient.isSameThread()) {
            return action.get();
        }
        checkTestThread("computeOnClient");
        if (!clientCanAcceptTasks) {
            throw new IllegalStateException("The client cannot currently accept test tasks");
        }
        return dispatch(action, TaskTarget.CLIENT);
    }

    public static <E extends Throwable> void runOnServer(MinecraftServer target, FailableRunnable<E> action) throws E {
        computeOnServer(target, () -> {
            action.run();
            return null;
        });
    }

    public static <T, E extends Throwable> T computeOnServer(MinecraftServer target, FailableSupplier<T, E> action)
            throws E {
        Objects.requireNonNull(target);
        Objects.requireNonNull(action);
        if (target.isSameThread()) {
            return action.get();
        }
        checkTestThread("computeOnServer");
        if (target != server || !serverCanAcceptTasks) {
            throw new IllegalStateException("The server cannot currently accept test tasks");
        }
        return dispatch(action, TaskTarget.SERVER);
    }

    private static <T, E extends Throwable> T dispatch(FailableSupplier<T, E> action, TaskTarget target) throws E {
        DispatchedTask<T> task = new DispatchedTask<>(target, action);
        if (!PENDING_TASK.compareAndSet(null, task)) {
            throw new IllegalStateException("Another Minecraft test task is already pending");
        }
        if (target.canAcceptTasks()) {
            target.semaphore().release();
        } else {
            task.cancel(
                    new IllegalStateException("The " + target.description() + " stopped before running a test task"));
        }
        return task.await();
    }

    private static void acceptTasks(TaskTarget target) {
        while (testThread != null) {
            acquire(target.semaphore());
            DispatchedTask<?> task = PENDING_TASK.get();
            if (task == null || task.target() != target) {
                return;
            }
            task.run();
        }
    }

    private static void stopClientAndReleaseThreads() {
        try {
            CLIENT_STOP_REQUESTED.set(true);
            if (clientCanAcceptTasks) {
                runOnClient(() -> client.stop());
            } else if (client != null) {
                client.execute(client::stop);
            }
        } finally {
            testThread = null;
            ACTIVE.set(false);
            clientCanAcceptTasks = false;
            PHASER.arriveAndDeregister();
            CLIENT_SEMAPHORE.release();
            SERVER_SEMAPHORE.release();
        }
    }

    private static void clientTerminated(Throwable failure) {
        if (!CLIENT_REGISTERED.compareAndSet(true, false)) {
            return;
        }
        clientCanAcceptTasks = false;
        PHASER.arriveAndDeregister();
        cancelPendingTask(TaskTarget.CLIENT, failure);
    }

    private static void cancelPendingTask(TaskTarget target, Throwable failure) {
        DispatchedTask<?> task = PENDING_TASK.get();
        if (task != null && task.target() == target) {
            task.cancel(failure);
        }
    }

    private static void enterPhase(int phase) {
        while (ACTIVE.get() && nextPhase() != phase) {
            PHASER.arriveAndAwaitAdvance();
        }
        if (ACTIVE.get()) {
            PHASER.arriveAndAwaitAdvance();
        }
    }

    private static int nextPhase() {
        return PHASER.getPhase() & PHASE_MASK;
    }

    private static void acquire(Semaphore semaphore) {
        boolean interrupted = false;
        while (true) {
            try {
                semaphore.acquire();
                break;
            } catch (InterruptedException exception) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void checkTestThread(String operation) {
        if (Thread.currentThread() != testThread) {
            throw new IllegalStateException(operation + " can only be called from the client-test thread");
        }
    }

    public static void rethrowFailure(Throwable failure) {
        if (failure == null) {
            return;
        }
        TestScheduler.<RuntimeException>throwAs(failure);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAs(Throwable failure) throws E {
        throw (E) failure;
    }

    enum TaskTarget {
        CLIENT(CLIENT_SEMAPHORE, "Minecraft client") {
            @Override
            boolean canAcceptTasks() {
                return CLIENT_REGISTERED.get() && clientCanAcceptTasks;
            }
        },
        SERVER(SERVER_SEMAPHORE, "logical server") {
            @Override
            boolean canAcceptTasks() {
                return SERVER_REGISTERED.get() && serverCanAcceptTasks;
            }
        };

        private final Semaphore semaphore;
        private final String description;

        TaskTarget(Semaphore semaphore, String description) {
            this.semaphore = semaphore;
            this.description = description;
        }

        Semaphore semaphore() {
            return semaphore;
        }

        String description() {
            return description;
        }

        abstract boolean canAcceptTasks();
    }

    static final class DispatchedTask<T> {
        private final TaskTarget target;
        private final FailableSupplier<T, ?> action;
        private final Semaphore completion = new Semaphore(0);

        private State state = State.PENDING;
        private T result;
        private Throwable failure;

        DispatchedTask(TaskTarget target, FailableSupplier<T, ?> action) {
            this.target = target;
            this.action = action;
        }

        TaskTarget target() {
            return target;
        }

        void run() {
            synchronized (this) {
                if (state != State.PENDING) {
                    return;
                }
                state = State.RUNNING;
            }
            T computed = null;
            Throwable thrown = null;
            try {
                computed = action.get();
            } catch (Throwable failure) {
                thrown = failure;
            }
            finishRun(computed, thrown);
        }

        void cancel(Throwable failure) {
            Objects.requireNonNull(failure);
            boolean completeNow = false;
            synchronized (this) {
                if (state == State.COMPLETED) {
                    return;
                }
                this.failure = ClientTestResources.append(this.failure, failure);
                if (state == State.PENDING) {
                    state = State.COMPLETED;
                    completeNow = true;
                }
            }
            if (completeNow) {
                releaseWaiter();
            }
        }

        <E extends Throwable> T await() throws E {
            acquire(completion);
            Throwable thrown;
            T value;
            synchronized (this) {
                thrown = failure;
                value = result;
            }
            if (thrown != null) {
                TestScheduler.<E>throwAs(thrown);
            }
            return value;
        }

        private void finishRun(T result, Throwable failure) {
            synchronized (this) {
                if (state != State.RUNNING) {
                    return;
                }
                state = State.COMPLETED;
                this.result = result;
                this.failure = ClientTestResources.append(this.failure, failure);
            }
            releaseWaiter();
        }

        private void releaseWaiter() {
            PENDING_TASK.compareAndSet(this, null);
            completion.release();
        }

        private enum State {
            PENDING,
            RUNNING,
            COMPLETED
        }
    }
}
