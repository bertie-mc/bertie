package me.shedaniel.autoconfig;

/** Minimal AutoConfig API fixture used without a runtime Cloth Config dependency. */
public interface ConfigData {
    default void validatePostLoad() throws ValidationException {}

    final class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }

        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }

        public ValidationException(Throwable cause) {
            super(cause);
        }
    }
}
