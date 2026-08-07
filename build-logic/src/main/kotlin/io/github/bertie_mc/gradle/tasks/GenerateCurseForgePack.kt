package io.github.bertie_mc.gradle.tasks

import com.google.gson.GsonBuilder
import io.github.bertie_mc.gradle.model.CurseForgeManifestArtifact
import io.github.bertie_mc.gradle.model.PackMetadata
import io.github.bertie_mc.gradle.model.PackwizArtifact
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

@CacheableTask
abstract class GenerateCurseForgePack : DefaultTask() {
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

    @get:Nested abstract val nativeArtifacts: ListProperty<CurseForgeManifestArtifact>

    @get:Nested abstract val embeddedArtifacts: ListProperty<PackwizArtifact>

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
        val native = nativeArtifacts.get().sortedWith(compareBy({ it.projectId }, { it.fileId }))
        val duplicateProjects =
            native
                .groupingBy(CurseForgeManifestArtifact::projectId)
                .eachCount()
                .filterValues { it > 1 }
                .keys
        require(duplicateProjects.isEmpty()) {
            "CurseForge pack contains multiple files for project IDs: " +
                duplicateProjects.sorted().joinToString()
        }
        val embedded = embeddedArtifacts.get().sortedBy(PackwizArtifact::id)
        val duplicatePaths =
            embedded
                .groupingBy { artifact -> "${artifact.destination}/${artifact.filename}" }
                .eachCount()
                .filterValues { count -> count > 1 }
                .keys
        require(duplicatePaths.isEmpty()) {
            "CurseForge embedded artifact paths collide: ${duplicatePaths.sorted().joinToString()}"
        }

        val manifest =
            linkedMapOf<String, Any>(
                "minecraft" to
                    linkedMapOf(
                        "version" to minecraftVersion.get(),
                        "modLoaders" to
                            listOf(
                                linkedMapOf(
                                    "id" to "neoforge-${neoForgeVersion.get()}",
                                    "primary" to true,
                                ),
                            ),
                    ),
                "manifestType" to "minecraftModpack",
                "manifestVersion" to 1,
                "name" to metadata.name,
                "version" to metadata.version,
                "author" to metadata.author,
                "files" to
                    native.map { artifact ->
                        linkedMapOf(
                            "projectID" to artifact.projectId,
                            "fileID" to artifact.fileId,
                            "required" to true,
                        )
                    },
                "overrides" to "overrides",
            )
        val manifestJson = GsonBuilder().setPrettyPrinting().create().toJson(manifest) + "\n"

        writeEmbeddingAudit(
            "release-curseforge",
            embedded,
            redistributionStrict.get(),
            redistributionEvidence.getOrElse(emptyList()),
            redistributionArtifacts.getOrElse(emptyList()),
            embeddingAuditFile.get().asFile.toPath(),
        )

        val output = outputFile.get().asFile.toPath()
        Files.createDirectories(output.parent)
        Files.deleteIfExists(output)
        ZipOutputStream(BufferedOutputStream(Files.newOutputStream(output))).use { zip ->
            zip.writeEntry("manifest.json", manifestJson.toByteArray(StandardCharsets.UTF_8))
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
