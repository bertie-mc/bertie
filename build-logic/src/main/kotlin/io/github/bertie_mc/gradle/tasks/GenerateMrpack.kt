package io.github.bertie_mc.gradle.tasks

import com.google.gson.GsonBuilder
import io.github.bertie_mc.gradle.model.MinecraftArtifactSide
import io.github.bertie_mc.gradle.model.PackMetadata
import io.github.bertie_mc.gradle.model.PackwizArtifact
import io.github.bertie_mc.gradle.model.PackwizProvider
import io.github.bertie_mc.gradle.model.RedistributionArtifactPolicy
import io.github.bertie_mc.gradle.model.RedistributionEvidence
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
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
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.BufferedOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipOutputStream

private const val MODRINTH_CDN_BASE_URL = "https://cdn.modrinth.com/data"

@CacheableTask
abstract class GenerateMrpack : DefaultTask() {
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

    @get:Input abstract val minecraftVersion: Property<String>

    @get:Input abstract val neoForgeVersion: Property<String>

    @get:Nested abstract val externalArtifacts: ListProperty<PackwizArtifact>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val localModFiles: ConfigurableFileCollection

    @get:Input abstract val redistributionStrict: Property<Boolean>

    @get:Nested abstract val redistributionEvidence: ListProperty<RedistributionEvidence>

    @get:Nested abstract val redistributionArtifacts: ListProperty<RedistributionArtifactPolicy>

    @get:OutputFile abstract val outputFile: RegularFileProperty

    @get:OutputFile abstract val embeddingAuditFile: RegularFileProperty

    @org.gradle.api.tasks.TaskAction
    fun generate() {
        val metadata =
            PackMetadata.parse(
                Files.readString(packProperties.get().asFile.toPath(), StandardCharsets.UTF_8),
            )
        val artifacts = externalArtifacts.get().sortedBy(PackwizArtifact::id)
        val duplicatePaths =
            artifacts
                .groupingBy { artifact -> "${artifact.destination}/${artifact.filename}" }
                .eachCount()
                .filterValues { count -> count > 1 }
                .keys
        require(duplicatePaths.isEmpty()) {
            "Modrinth pack artifact paths collide: ${duplicatePaths.sorted().joinToString()}"
        }

        val indexFiles =
            artifacts
                .filter { artifact -> artifact.provider == PackwizProvider.MODRINTH }
                .map { artifact ->
                    val path = "${artifact.destination}/${artifact.filename}"
                    mapOf(
                        "path" to path,
                        "hashes" to
                            mapOf(
                                "sha1" to clientPackFileHash(artifact.file.toPath(), "SHA-1"),
                                "sha512" to clientPackFileHash(artifact.file.toPath(), "SHA-512"),
                            ),
                        "env" to environment(artifact.side),
                        "downloads" to
                            listOf(
                                "$MODRINTH_CDN_BASE_URL/${urlPath(artifact.projectId)}/versions/" +
                                    "${urlPath(artifact.versionId)}/${urlPath(artifact.filename)}",
                            ),
                        "fileSize" to artifact.file.length(),
                    )
                }
        val index =
            linkedMapOf<String, Any>(
                "formatVersion" to 1,
                "game" to "minecraft",
                "versionId" to metadata.version,
                "name" to metadata.name,
                "summary" to metadata.description,
                "files" to indexFiles,
                "dependencies" to
                    linkedMapOf(
                        "minecraft" to minecraftVersion.get(),
                        "neoforge" to neoForgeVersion.get(),
                    ),
            )
        val indexJson = GsonBuilder().setPrettyPrinting().create().toJson(index) + "\n"

        val embedded = artifacts.filter { it.provider != PackwizProvider.MODRINTH }
        val auditPath = embeddingAuditFile.get().asFile.toPath()
        writeEmbeddingAudit(
            "release-modrinth",
            embedded,
            redistributionStrict.get(),
            redistributionEvidence.getOrElse(emptyList()),
            redistributionArtifacts.getOrElse(emptyList()),
            auditPath,
        )

        val output = outputFile.get().asFile.toPath()
        Files.createDirectories(output.parent)
        Files.deleteIfExists(output)
        ZipOutputStream(BufferedOutputStream(Files.newOutputStream(output))).use { zip ->
            zip.writeEntry("modrinth.index.json", indexJson.toByteArray(StandardCharsets.UTF_8))
            writeDirectory(zip, contentDirectory.get().asFile.toPath(), "overrides/config")
            if (datapackDirectory.isPresent) {
                writeDirectory(zip, datapackDirectory.get().asFile.toPath(), "overrides/datapacks")
            }
            if (resourcepackDirectory.isPresent) {
                writeDirectory(zip, resourcepackDirectory.get().asFile.toPath(), "overrides/resourcepacks")
            }
            localModFiles.files.sortedBy { it.name }.forEach { file ->
                zip.writeFile("overrides/mods/${file.name}", file.toPath())
            }
            embedded.forEach { artifact ->
                zip.writeFile(
                    "overrides/${artifact.destination}/${artifact.filename}",
                    artifact.file.toPath(),
                )
            }
        }
    }
}

private fun environment(side: MinecraftArtifactSide): Map<String, String> =
    when (side) {
        MinecraftArtifactSide.BOTH -> mapOf("client" to "required", "server" to "required")
        MinecraftArtifactSide.CLIENT -> mapOf("client" to "required", "server" to "unsupported")
        MinecraftArtifactSide.SERVER -> mapOf("client" to "unsupported", "server" to "required")
    }

private fun urlPath(value: String): String =
    java.net.URLEncoder
        .encode(value, StandardCharsets.UTF_8)
        .replace("+", "%20")
