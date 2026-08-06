package io.github.bertie_mc.configmigrations.integration.autoconfig;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest.Change;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Recursive merge over AutoConfig's serializer-independent Java data model. */
final class AutoConfigTreeMerge {
    private AutoConfigTreeMerge() {}

    static void apply(Object target, List<Change> changes) {
        for (Change change : changes) {
            mergeObject(target, change.fragment());
        }
    }

    private static void mergeObject(Object target, UnmodifiableConfig fragment) {
        for (var entry : fragment.entrySet()) {
            Field field = field(target.getClass(), entry.getKey());
            try {
                field.setAccessible(true);
                Object current = field.get(target);
                Object merged = mergeValue(current, entry.getValue(), field.getGenericType());
                if (merged != current) {
                    field.set(target, merged);
                }
            } catch (ReflectiveOperationException exception) {
                throw new ConfigMigrationException("Failed to merge AutoConfig field " + field.getName(), exception);
            }
        }
    }

    private static Object mergeValue(Object current, Object fragment, Type type) throws ReflectiveOperationException {
        if (!(fragment instanceof UnmodifiableConfig table)) {
            return convert(fragment, type);
        }

        Class<?> raw = raw(type);
        if (Map.class.isAssignableFrom(raw)) {
            Map<Object, Object> map = current == null ? new LinkedHashMap<>() : map(current);
            mergeMap(map, table, type);
            return map;
        }

        Object object = current == null ? construct(raw) : current;
        mergeObject(object, table);
        return object;
    }

    private static void mergeMap(Map<Object, Object> target, UnmodifiableConfig fragment, Type type)
            throws ReflectiveOperationException {
        Type keyType = argument(type, 0);
        Type valueType = argument(type, 1);
        for (var entry : fragment.entrySet()) {
            Object key = convert(entry.getKey(), keyType);
            Object current = target.get(key);
            target.put(key, mergeValue(current, entry.getValue(), valueType));
        }
    }

    private static Object convert(Object value, Type type) throws ReflectiveOperationException {
        if (type == Object.class) {
            return copy(value);
        }
        Class<?> raw = raw(type);
        if (value instanceof UnmodifiableConfig table) {
            return mergeValue(null, table, type);
        }
        if (value instanceof List<?> list) {
            if (raw.isArray()) {
                Class<?> component = raw.getComponentType();
                Object array = Array.newInstance(component, list.size());
                for (int index = 0; index < list.size(); index++) {
                    Array.set(array, index, convert(list.get(index), component));
                }
                return array;
            }
            if (List.class.isAssignableFrom(raw)) {
                Type elementType = argument(type, 0);
                List<Object> converted = new ArrayList<>(list.size());
                for (Object element : list) {
                    converted.add(convert(element, elementType));
                }
                return converted;
            }
        }
        if (raw.isEnum() && value instanceof String name) {
            return enumValue(raw, name);
        }
        if (value instanceof Number number && numberType(raw)) {
            return number(number, raw);
        }
        if ((raw == boolean.class || raw == Boolean.class) && value instanceof Boolean) {
            return value;
        }
        if (raw == String.class && value instanceof String) {
            return value;
        }
        if (raw.isInstance(value)) {
            return value;
        }
        throw new ConfigMigrationException("Cannot convert " + value + " to " + raw.getName());
    }

    private static Object copy(Object value) throws ReflectiveOperationException {
        if (value instanceof UnmodifiableConfig table) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            mergeMap(copy, table, Object.class);
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object element : list) {
                copy.add(copy(element));
            }
            return copy;
        }
        return value;
    }

    private static Field field(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                if (!Modifier.isStatic(field.getModifiers())) {
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
                // Continue through inherited config fields.
            }
        }
        throw new ConfigMigrationException("Unknown AutoConfig field " + type.getName() + "." + name);
    }

    private static Object construct(Class<?> type) throws ReflectiveOperationException {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> map(Object value) {
        return (Map<Object, Object>) value;
    }

    private static Type argument(Type type, int index) {
        if (type instanceof ParameterizedType parameterized) {
            return parameterized.getActualTypeArguments()[index];
        }
        return Object.class;
    }

    private static Class<?> raw(Type type) {
        if (type instanceof Class<?> raw) {
            return raw;
        }
        if (type instanceof ParameterizedType parameterized) {
            return (Class<?>) parameterized.getRawType();
        }
        return Object.class;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumValue(Class<?> type, String name) {
        return Enum.valueOf((Class<? extends Enum>) type, name);
    }

    private static boolean numberType(Class<?> type) {
        return type == byte.class
                || type == Byte.class
                || type == short.class
                || type == Short.class
                || type == int.class
                || type == Integer.class
                || type == long.class
                || type == Long.class
                || type == float.class
                || type == Float.class
                || type == double.class
                || type == Double.class;
    }

    private static Object number(Number value, Class<?> type) {
        if (type == byte.class || type == Byte.class) {
            return value.byteValue();
        }
        if (type == short.class || type == Short.class) {
            return value.shortValue();
        }
        if (type == int.class || type == Integer.class) {
            return value.intValue();
        }
        if (type == long.class || type == Long.class) {
            return value.longValue();
        }
        if (type == float.class || type == Float.class) {
            return value.floatValue();
        }
        return value.doubleValue();
    }
}
