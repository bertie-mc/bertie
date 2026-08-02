package io.github.bertie_mc.configmigrations;

/** A manifest or state value that cannot be executed. */
public final class ConfigMigrationException extends RuntimeException {
    public ConfigMigrationException(String message) {
        super(message);
    }

    public ConfigMigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
