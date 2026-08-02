package me.shedaniel.autoconfig.serializer;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

/** Minimal persistence API matching AutoConfig's public serializer contract. */
public interface ConfigSerializer<T extends ConfigData> {
    void serialize(T config) throws SerializationException;

    T deserialize() throws SerializationException;

    T createDefault();

    interface Factory<T extends ConfigData> {
        ConfigSerializer<T> create(Config definition, Class<T> configClass);
    }

    final class SerializationException extends Exception {
        public SerializationException(Throwable cause) {
            super(cause);
        }
    }
}
