package io.github.bertie_mc.testing.client.driver;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Resources created by one client-test method, closed in reverse creation order. */
final class ClientTestResources implements AutoCloseable {
    private final Deque<AutoCloseable> resources = new ArrayDeque<>();
    private boolean closed;

    synchronized <T extends AutoCloseable> T own(T resource) {
        Objects.requireNonNull(resource);
        if (closed) {
            closeLateResource(resource);
            throw new IllegalStateException("The client-test context is already closed");
        }
        resources.addLast(resource);
        return resource;
    }

    @Override
    public void close() {
        Throwable failure = null;
        while (true) {
            AutoCloseable resource;
            synchronized (this) {
                closed = true;
                resource = resources.pollLast();
            }
            if (resource == null) {
                break;
            }
            try {
                resource.close();
            } catch (Throwable closeFailure) {
                failure = append(failure, closeFailure);
            }
        }
        rethrow(failure);
    }

    private static void closeLateResource(AutoCloseable resource) {
        try {
            resource.close();
        } catch (RuntimeException | Error failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot close a late client-test resource", failure);
        }
    }

    static Throwable append(Throwable existing, Throwable additional) {
        if (additional == null) {
            return existing;
        }
        if (existing == null) {
            return additional;
        }
        if (existing != additional) {
            existing.addSuppressed(additional);
        }
        return existing;
    }

    static void rethrow(Throwable failure) {
        if (failure == null) {
            return;
        }
        if (failure instanceof RuntimeException runtime) {
            throw runtime;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        throw new IllegalStateException("Cannot close client-test resources", failure);
    }
}
