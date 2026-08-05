package io.github.bertie_mc.alexscavesworldgenfix.logic;

import java.util.AbstractList;
import java.util.List;
import java.util.Objects;

/**
 * A read-only view of a list that answers an out-of-range index with a fallback instead of throwing.
 *
 * <p>This exists for one instruction: {@code stepFeatureData.features().get(index)} in
 * {@code ChunkGenerator#applyBiomeDecoration}. The index arrives from
 * {@code StepFeatureData#indexMapping}, an identity lookup that returns {@code -1} for a feature it
 * has never been shown. Alex's Caves redirects that {@code get} through a clamp which turns
 * {@code -1} into {@code 0} - correct on a populated list, fatal on an empty one.
 *
 * <p>Reporting the delegate's real {@link #size()} is deliberate: the clamp reads it, and changing
 * it would change which feature the clamp picks on lists that are not empty. Only the throw is
 * removed.
 */
public final class TolerantIndexList<T> extends AbstractList<T> {

    private final List<T> delegate;
    private final T fallback;
    private final Runnable onFallback;

    private TolerantIndexList(List<T> delegate, T fallback, Runnable onFallback) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.onFallback = Objects.requireNonNull(onFallback, "onFallback");
    }

    /**
     * Wrap {@code delegate} so that any index outside it yields {@code fallback}.
     *
     * @param onFallback run once per substituted index, for diagnostics
     */
    public static <T> List<T> wrap(List<T> delegate, T fallback, Runnable onFallback) {
        return new TolerantIndexList<>(delegate, fallback, onFallback);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public T get(int index) {
        if (index >= 0 && index < delegate.size()) {
            return delegate.get(index);
        }
        onFallback.run();
        return fallback;
    }
}
