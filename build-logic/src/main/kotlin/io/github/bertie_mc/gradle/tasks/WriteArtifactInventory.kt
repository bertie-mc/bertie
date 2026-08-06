package io.github.bertie_mc.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files

abstract class WriteArtifactInventory : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val artifacts: ConfigurableFileCollection

    @get:Input
    abstract val artifactIdsByFileName: MapProperty<String, String>

    @get:Input
    abstract val fakePackArtifactIds: SetProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun write() {
        val ids = artifactIdsByFileName.get()
        val fakePacks = fakePackArtifactIds.get()
        val inventory =
            artifacts.files
                .map { artifact ->
                    val id =
                        ids[artifact.name]
                            ?: error("Resolved undeclared artifact: ${artifact.name}")
                    "$id\t${id in fakePacks}\t${artifact.absolutePath}"
                }.sorted()
                .joinToString(separator = "\n", postfix = "\n")
        val destination = outputFile.get().asFile.toPath()
        Files.createDirectories(destination.parent)
        Files.writeString(destination, inventory, StandardCharsets.UTF_8)
    }
}
