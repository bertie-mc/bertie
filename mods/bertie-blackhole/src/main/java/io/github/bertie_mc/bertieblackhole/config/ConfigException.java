package io.github.bertie_mc.bertieblackhole.config;

/** Thrown for anything wrong in bertieblackhole.json; the message is shown in the log verbatim. */
public class ConfigException extends RuntimeException {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
