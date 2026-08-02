package io.github.bertie_mc.configmigrations.integration.fzzyconfig;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.peanuuutz.tomlkt.TomlArray;
import net.peanuuutz.tomlkt.TomlElement;
import net.peanuuutz.tomlkt.TomlLiteral;
import net.peanuuutz.tomlkt.TomlTable;

/** Small bridge to Fzzy Config's FileType/TomlElement file model. */
final class FzzyConfigFile {
    private final Object api;
    private final Object classInstance;
    private final Object preferredType;
    private final Path directory;
    private final String name;
    private final Path canonicalPath;
    private final Method suffix;
    private final Method decode;
    private final Method encode;
    private final Method resultGet;

    private FzzyConfigFile(
            Path configDirectory,
            Object api,
            Object classInstance,
            String name,
            String folder,
            String subfolder) {
        this.api = api;
        this.classInstance = classInstance;
        this.name = name;
        try {
            preferredType = classInstance.getClass().getMethod("fileType").invoke(classInstance);
            Class<?> fileType = preferredType.getClass();

            suffix = fileType.getMethod("suffix");
            decode = fileType.getMethod("decode", String.class);
            encode = fileType.getMethod("encode", TomlElement.class);
            Class<?> result = decode.getReturnType();
            resultGet = result.getMethod("get");

            directory = directory(configDirectory, folder, subfolder);
            canonicalPath = directory.resolve(name + suffix(preferredType))
                    .toAbsolutePath()
                    .normalize();
        } catch (ReflectiveOperationException exception) {
            throw failure("open Fzzy Config file", exception);
        }
    }

    static FzzyConfigFile open(
            Path configDirectory,
            Object api,
            Object classInstance,
            String name,
            String folder,
            String subfolder) {
        return new FzzyConfigFile(
                configDirectory, api, classInstance, name, folder, subfolder);
    }

    Path canonicalPath() {
        return canonicalPath;
    }

    void mergeAndWrite(List<MigrationManifest.Change> changes) {
        Input input = input();
        TomlElement document = Files.exists(input.path())
                ? decode(input.type(), read(input.path()))
                : new TomlTable(Map.of(), Map.of());
        if (!(document instanceof TomlTable)) {
            throw new ConfigMigrationException("Fzzy Config root is not a table: " + input.path());
        }
        for (MigrationManifest.Change change : changes) {
            document = merge(document, change.fragment());
        }
        write(input.path(), encode(input.type(), document));
    }

    private Input input() {
        if (Files.exists(canonicalPath)) {
            return new Input(canonicalPath, preferredType);
        }
        try {
            Object values = preferredType.getClass().getMethod("values").invoke(null);
            for (int index = 0; index < Array.getLength(values); index++) {
                Object type = Array.get(values, index);
                if (type == preferredType) {
                    continue;
                }
                Path candidate = directory.resolve(name + suffix(type));
                if (Files.exists(candidate)) {
                    return new Input(candidate, type);
                }
            }
            Input compatibility = compatibilityInput();
            return compatibility != null
                    ? compatibility
                    : new Input(canonicalPath, preferredType);
        } catch (ReflectiveOperationException exception) {
            throw failure("discover Fzzy Config file " + canonicalPath, exception);
        }
    }

    private Input compatibilityInput() throws ReflectiveOperationException {
        ClassLoader loader = api.getClass().getClassLoader();
        Class<?> kClass = Class.forName("kotlin.reflect.KClass", false, loader);
        Class<?> reflection = Class.forName("kotlin.jvm.internal.Reflection", false, loader);
        Object configClass = reflection
                .getMethod("getOrCreateKotlinClass", Class.class)
                .invoke(null, classInstance.getClass());
        Method getCompat = api.getClass().getDeclaredMethod("getCompat", kClass);
        getCompat.setAccessible(true);
        Object compatibility = getCompat.invoke(api, configClass);
        Object first = compatibility.getClass().getMethod("getFirst").invoke(compatibility);
        if (!(first instanceof File file) || !file.exists()) {
            return null;
        }
        Object type = compatibility.getClass().getMethod("getSecond").invoke(compatibility);
        return new Input(file.toPath().toAbsolutePath().normalize(), type);
    }

    private TomlElement merge(TomlElement target, UnmodifiableConfig fragment) {
        if (!(target instanceof TomlTable targetTable)) {
            return fromFragment(fragment);
        }
        Map<String, TomlElement> merged = new LinkedHashMap<>(targetTable.getContent());
        for (var entry : fragment.entrySet()) {
            Object value = entry.getValue();
            TomlElement current = merged.get(entry.getKey());
            if (value instanceof UnmodifiableConfig nested && current instanceof TomlTable) {
                merged.put(entry.getKey(), merge(current, nested));
            } else {
                merged.put(entry.getKey(), element(value));
            }
        }
        return new TomlTable(merged, targetTable.getAnnotations());
    }

    private TomlTable fromFragment(UnmodifiableConfig fragment) {
        Map<String, TomlElement> entries = new LinkedHashMap<>();
        fragment.entrySet().forEach(entry -> entries.put(entry.getKey(), element(entry.getValue())));
        return new TomlTable(entries, Map.of());
    }

    private TomlElement element(Object value) {
        if (value instanceof UnmodifiableConfig config) {
            return fromFragment(config);
        }
        if (value instanceof List<?> list) {
            List<TomlElement> values = new ArrayList<>(list.size());
            for (Object entry : list) {
                values.add(element(entry));
            }
            return new TomlArray(
                    values,
                    Collections.nCopies(values.size(), List.<Annotation>of()));
        }
        if (value instanceof String string) {
            return new TomlLiteral(string, TomlLiteral.Type.String);
        }
        if (value instanceof Boolean bool) {
            return new TomlLiteral(bool.toString(), TomlLiteral.Type.Boolean);
        }
        if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) {
            return new TomlLiteral(value.toString(), TomlLiteral.Type.Float);
        }
        if (value instanceof Number number) {
            return new TomlLiteral(number.toString(), TomlLiteral.Type.Integer);
        }
        throw new ConfigMigrationException("Unsupported Fzzy Config migration value " + value);
    }

    private String suffix(Object type) {
        try {
            return (String) suffix.invoke(type);
        } catch (ReflectiveOperationException exception) {
            throw failure("read Fzzy Config file suffix", exception);
        }
    }

    private TomlElement decode(Object type, String contents) {
        try {
            return (TomlElement) value(decode.invoke(type, contents));
        } catch (ReflectiveOperationException exception) {
            throw failure("decode Fzzy Config " + canonicalPath, exception);
        }
    }

    private String encode(Object type, TomlElement document) {
        try {
            return (String) value(encode.invoke(type, document));
        } catch (ReflectiveOperationException exception) {
            throw failure("encode Fzzy Config " + canonicalPath, exception);
        }
    }

    private Object value(Object result) throws ReflectiveOperationException {
        return resultGet.invoke(result);
    }

    private String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ConfigMigrationException("Failed to read Fzzy Config " + path, exception);
        }
    }

    private void write(Path path, String contents) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, contents, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ConfigMigrationException("Failed to write Fzzy Config " + path, exception);
        }
    }

    private static Path directory(Path configDirectory, String folder, String subfolder) {
        Path directory = configDirectory;
        if (!folder.isEmpty()) {
            directory = directory.resolve(folder);
        }
        if (!subfolder.isEmpty()) {
            directory = directory.resolve(subfolder);
        }
        return directory;
    }

    private static ConfigMigrationException failure(String action, ReflectiveOperationException exception) {
        Throwable cause = exception instanceof InvocationTargetException invocation
                ? invocation.getTargetException()
                : exception;
        return new ConfigMigrationException("Failed to " + action, cause);
    }

    private record Input(Path path, Object type) {
    }
}
