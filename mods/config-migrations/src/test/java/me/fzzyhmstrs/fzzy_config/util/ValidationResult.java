package me.fzzyhmstrs.fzzy_config.util;

public final class ValidationResult<T> {
    private final T value;
    private final boolean error;

    private ValidationResult(T value, boolean error) {
        this.value = value;
        this.error = error;
    }

    public static <T> ValidationResult<T> success(T value) {
        return new ValidationResult<>(value, false);
    }

    public static <T> ValidationResult<T> error(T fallback) {
        return new ValidationResult<>(fallback, true);
    }

    public boolean isError() {
        return error;
    }

    public T get() {
        return value;
    }
}
