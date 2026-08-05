package io.github.bertie_mc.gradle.tasks

import net.neoforged.nfrtgradle.NeoFormRuntimeTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class DownloadMinecraftArtifacts : NeoFormRuntimeTask() {
    @get:Input
    abstract val minecraftVersion: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun download() {
        val output = outputDirectory.get().asFile
        val arguments = mutableListOf(
            "download-artifacts",
            "--minecraft-version",
            minecraftVersion.get(),
            "--write-version-manifest",
            output.resolve("version.json").absolutePath,
        )
        mapOf(
            "client" to "client.jar",
            "server" to "server.jar",
            "client_mappings" to "client-mappings.txt",
            "server_mappings" to "server-mappings.txt",
        ).forEach { (artifact, filename) ->
            arguments += "--write-artifact"
            arguments += "$artifact:${output.resolve(filename).absolutePath}"
        }
        run(arguments)
    }
}
