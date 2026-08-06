package me.fzzyhmstrs.fzzy_config.impl;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import kotlin.Pair;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.Reflection;
import kotlin.reflect.KClass;
import me.fzzyhmstrs.fzzy_config.api.FileType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import net.neoforged.fml.loading.FMLPaths;
import net.peanuuutz.tomlkt.TomlTable;

public final class ConfigApiImpl {
    public static final ConfigApiImpl INSTANCE = new ConfigApiImpl();

    private Runnable beforeReturn = () -> {};
    private int nativeLoads;
    private CompletableFuture<Void> lastWrite = CompletableFuture.completedFuture(null);
    private File compatibilityFile;
    private FileType compatibilityType = FileType.TOML;

    private ConfigApiImpl() {}

    public <T extends Config> T readOrCreateAndValidate$fzzy_config(
            Function0<? extends T> configClass, T classInstance, String name, String folder, String subfolder) {
        nativeLoads++;
        Path directory = FMLPaths.CONFIGDIR.get();
        if (!folder.isEmpty()) {
            directory = directory.resolve(folder);
        }
        if (!subfolder.isEmpty()) {
            directory = directory.resolve(subfolder);
        }
        FileType preferred = classInstance.fileType();
        Path canonical = directory.resolve(name + preferred.suffix());
        Path input = canonical;
        if (!Files.exists(input)) {
            for (FileType type : FileType.values()) {
                Path candidate = directory.resolve(name + type.suffix());
                if (Files.exists(candidate)) {
                    input = candidate;
                    break;
                }
            }
        }

        try {
            T loaded = classInstance;
            if (Files.exists(input)) {
                TomlTable document =
                        (TomlTable) type(input).decode(Files.readString(input)).get();
                loaded.fixtureDocument(document);
            } else {
                Pair<File, FileType> compatibility =
                        getCompat(Reflection.getOrCreateKotlinClass(classInstance.getClass()));
                if (compatibility.getFirst() != null && compatibility.getFirst().exists()) {
                    input = compatibility.getFirst().toPath();
                    TomlTable document = (TomlTable) compatibility
                            .getSecond()
                            .decode(Files.readString(input))
                            .get();
                    loaded.fixtureDocument(document);
                }
            }
            writeFile(loaded, new FileResult(input, canonical, preferred), name, "fixture write");
            beforeReturn.run();
            return loaded;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public void fixtureBeforeReturn(Runnable action) {
        beforeReturn = action;
    }

    public int fixtureNativeLoads() {
        return nativeLoads;
    }

    public void fixtureReset() {
        lastWrite.join();
        beforeReturn = () -> {};
        nativeLoads = 0;
        compatibilityFile = null;
        compatibilityType = FileType.TOML;
    }

    public void fixtureAwaitWrites() {
        lastWrite.join();
    }

    public void fixtureCompatibility(Path file, FileType type) {
        compatibilityFile = file.toFile();
        compatibilityType = type;
    }

    private Pair<File, FileType> getCompat(KClass<?> configClass) {
        return new Pair<>(compatibilityFile, compatibilityType);
    }

    private void writeFile(Object config, FileResult files, String name, String error) {
        lastWrite = CompletableFuture.runAsync(
                () -> {
                    try {
                        Files.createDirectories(files.output().getParent());
                        Files.writeString(
                                files.output(),
                                files.outputType()
                                        .encode(((Config) config).fixtureDocument())
                                        .get());
                        if (!files.input().equals(files.output())) {
                            Files.deleteIfExists(files.input());
                        }
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                },
                ForkJoinPool.commonPool());
    }

    private static FileType type(Path path) {
        for (FileType type : FileType.values()) {
            if (path.getFileName().toString().endsWith(type.suffix())) {
                return type;
            }
        }
        throw new IllegalArgumentException(path.toString());
    }

    private record FileResult(Path input, Path output, FileType outputType) {}
}
