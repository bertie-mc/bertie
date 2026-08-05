package io.github.bertie_mc.gradle.model

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File
import java.io.Serializable
import java.io.StringReader
import java.util.Properties

data class PackMetadata(
    val name: String,
    val author: String,
    val version: String,
    val description: String,
) {
    companion object {
        fun parse(contents: String): PackMetadata {
            val properties = Properties().apply {
                StringReader(contents).use(::load)
            }
            fun required(name: String): String =
                properties.getProperty(name)?.takeIf(String::isNotBlank)
                    ?: error("Required pack property '$name' is missing")

            return PackMetadata(
                name = required("name"),
                author = required("author"),
                version = required("version"),
                description = required("description"),
            )
        }
    }
}

enum class PackwizProvider {
    MODRINTH,
    CURSEFORGE,
}

data class PackwizArtifact(
    @get:Input val id: String,
    @get:Input val displayName: String,
    @get:Input val installedName: String,
    @get:Input val destination: String,
    @get:Input val side: MinecraftArtifactSide,
    @get:Input val provider: PackwizProvider,
    @get:Input val projectId: String,
    @get:Input val versionId: String,
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    val file: File,
) : Serializable
