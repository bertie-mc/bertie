package com.teamresourceful.resourcefulconfig.common.config;

import java.util.Locale;

public final class ParsingUtils {
    private ParsingUtils() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Enum<?> parseEnum(Class<?> type, String value) {
        try {
            return Enum.valueOf((Class<? extends Enum>) type, value.toUpperCase(Locale.ROOT));
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
