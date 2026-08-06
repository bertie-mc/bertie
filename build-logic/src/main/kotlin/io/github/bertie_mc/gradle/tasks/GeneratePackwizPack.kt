package io.github.bertie_mc.gradle.tasks

import io.github.bertie_mc.gradle.model.PackMetadata
import io.github.bertie_mc.gradle.model.PackwizArtifact
import io.github.bertie_mc.gradle.model.PackwizProvider
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import javax.inject.Inject

private val PACKWIZ_ARTIFACT_ID = Regex("[a-z0-9][a-z0-9_-]*")
private const val MODRINTH_CDN_BASE_URL = "https://cdn.modrinth.com/data"

@CacheableTask
abstract class GeneratePackwizPack : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val packProperties: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val contentDirectory: DirectoryProperty

    @get:InputDirectory
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val datapackDirectory: DirectoryProperty

    @get:InputDirectory
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resourcepackDirectory: DirectoryProperty

    @get:Input
    abstract val minecraftVersion: Property<String>

    @get:Input
    abstract val neoForgeVersion: Property<String>

    @get:Nested
    abstract val packArtifacts: ListProperty<PackwizArtifact>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val localModFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun generate() {
        val output = outputDirectory.get().asFile.toPath()
        fileSystemOperations.delete { delete(output) }
        Files.createDirectories(output)
        fileSystemOperations.copy {
            from(contentDirectory)
            into(output.resolve("config"))
        }
        if (datapackDirectory.isPresent) {
            fileSystemOperations.copy {
                from(datapackDirectory)
                into(output.resolve("datapacks"))
            }
        }
        if (resourcepackDirectory.isPresent) {
            fileSystemOperations.copy {
                from(resourcepackDirectory)
                into(output.resolve("resourcepacks"))
            }
        }
        fileSystemOperations.copy {
            from(localModFiles)
            into(output.resolve("mods"))
        }

        val artifacts = packArtifacts.get().sortedBy(PackwizArtifact::id)
        artifacts.forEach { artifact ->
            require(PACKWIZ_ARTIFACT_ID.matches(artifact.id)) {
                "Pack artifact id '${artifact.id}' is unsafe"
            }
        }
        val duplicateMetafilePaths =
            artifacts
                .groupingBy { artifact ->
                    "${artifact.destination}/${artifact.id}.pw.toml"
                }.eachCount()
                .filterValues { count -> count > 1 }
                .keys
                .sorted()
        require(duplicateMetafilePaths.isEmpty()) {
            "Pack artifact metafile paths collide: ${duplicateMetafilePaths.joinToString()}"
        }
        artifacts.forEach { artifact ->
            val path = output.resolve(artifact.destination).resolve("${artifact.id}.pw.toml")
            write(path, metafile(artifact))
        }

        val indexedFiles =
            Files.walk(output).use { paths ->
                paths
                    .filter(Files::isRegularFile)
                    .filter { it.fileName.toString() !in setOf("index.toml", "pack.toml") }
                    .map { output.relativize(it) }
                    .sorted(compareBy(Path::toString))
                    .toList()
            }
        val index =
            buildString {
                hashSection("sha256")
                indexedFiles.forEach { relative ->
                    append("\n[[files]]\n")
                    append("file = ").append(toml(relative.toString().replace(File.separatorChar, '/'))).append('\n')
                    append("hash = ").append(toml(hash(output.resolve(relative), "SHA-256"))).append('\n')
                    if (relative.fileName.toString().endsWith(".pw.toml")) {
                        append("metafile = true\n")
                    }
                }
            }
        val indexFile = output.resolve("index.toml")
        write(indexFile, index)

        val identity =
            PackMetadata.parse(
                Files.readString(packProperties.get().asFile.toPath(), StandardCharsets.UTF_8),
            )
        val pack =
            buildString {
                append("name = ").append(toml(identity.name)).append('\n')
                append("author = ").append(toml(identity.author)).append('\n')
                append("version = ").append(toml(identity.version)).append('\n')
                append("description = ").append(toml(identity.description)).append('\n')
                append("pack-format = \"packwiz:1.1.0\"\n")
                append("\n[index]\n")
                append("file = \"index.toml\"\n")
                append("hash-format = \"sha256\"\n")
                append("hash = ").append(toml(hash(indexFile, "SHA-256"))).append('\n')
                append("\n[versions]\n")
                append("minecraft = ").append(toml(minecraftVersion.get())).append('\n')
                append("neoforge = ").append(toml(neoForgeVersion.get())).append('\n')
            }
        write(output.resolve("pack.toml"), pack)
    }

    private fun metafile(artifact: PackwizArtifact): String =
        buildString {
            append("name = ").append(toml(artifact.displayName)).append('\n')
            append("filename = ").append(toml(artifact.filename)).append('\n')
            append("side = ").append(toml(artifact.side.value)).append('\n')
            append("\n[download]\n")
            when (artifact.provider) {
                PackwizProvider.CURSEFORGE -> appendCurseForge(artifact)
                PackwizProvider.MODRINTH -> appendModrinth(artifact)
            }
        }

    private fun StringBuilder.appendCurseForge(artifact: PackwizArtifact) {
        append("hash-format = \"sha1\"\n")
        append("hash = ").append(toml(hash(artifact.file.toPath(), "SHA-1"))).append('\n')
        append("mode = \"metadata:curseforge\"\n")
        append("\n[update]\n")
        append("[update.curseforge]\n")
        append("file-id = ").append(artifact.versionId).append('\n')
        append("project-id = ").append(artifact.projectId).append('\n')
    }

    private fun StringBuilder.appendModrinth(artifact: PackwizArtifact) {
        val url =
            "$MODRINTH_CDN_BASE_URL/${urlPath(artifact.projectId)}/versions/" +
                "${urlPath(artifact.versionId)}/${urlPath(artifact.filename)}"
        append("url = ").append(toml(url)).append('\n')
        append("hash-format = \"sha512\"\n")
        append("hash = ").append(toml(hash(artifact.file.toPath(), "SHA-512"))).append('\n')
        append("\n[update]\n")
        append("[update.modrinth]\n")
        append("mod-id = ").append(toml(artifact.projectId)).append('\n')
        append("version = ").append(toml(artifact.versionId)).append('\n')
    }

    private fun StringBuilder.hashSection(format: String) {
        append("hash-format = ").append(toml(format)).append('\n')
    }
}

private fun write(
    path: Path,
    contents: String,
) {
    Files.createDirectories(path.parent)
    Files.writeString(path, contents, StandardCharsets.UTF_8)
}

private fun hash(
    path: Path,
    algorithm: String,
): String {
    val digest = MessageDigest.getInstance(algorithm)
    Files.newInputStream(path).buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}

private fun toml(value: String): String =
    buildString {
        append('"')
        value.forEach { character ->
            append(
                when (character) {
                    '\\' -> "\\\\"
                    '"' -> "\\\""
                    '\n' -> "\\n"
                    '\r' -> "\\r"
                    '\t' -> "\\t"
                    else -> character
                },
            )
        }
        append('"')
    }

private fun urlPath(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
