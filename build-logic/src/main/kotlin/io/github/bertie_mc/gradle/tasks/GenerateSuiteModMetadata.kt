package io.github.bertie_mc.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateSuiteModMetadata : DefaultTask() {
    @get:Input
    abstract val modId: Property<String>

    @get:Input
    abstract val displayName: Property<String>

    @get:Input
    abstract val descriptionText: Property<String>

    @get:Input
    abstract val license: Property<String>

    @get:Input
    abstract val minecraftVersionRange: Property<String>

    @get:Input
    abstract val neoForgeVersionRange: Property<String>

    @get:Input
    abstract val javaFmlLoaderVersionRange: Property<String>

    @get:Input
    @get:Optional
    abstract val testedModId: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val metadata = outputDirectory.file("META-INF/neoforge.mods.toml").get().asFile
        metadata.parentFile.mkdirs()
        metadata.writeText(
            renderSuiteModMetadata(
                modId = modId.get(),
                displayName = displayName.get(),
                description = descriptionText.get(),
                license = license.get(),
                minecraftVersionRange = minecraftVersionRange.get(),
                neoForgeVersionRange = neoForgeVersionRange.get(),
                javaFmlLoaderVersionRange = javaFmlLoaderVersionRange.get(),
                testedModId = testedModId.orNull,
            ),
        )
    }
}

internal fun renderSuiteModMetadata(
    modId: String,
    displayName: String,
    description: String,
    license: String,
    minecraftVersionRange: String,
    neoForgeVersionRange: String,
    javaFmlLoaderVersionRange: String,
    testedModId: String? = null,
): String {
    val testedModDependency =
        testedModId?.let {
            """
            [[dependencies.${tomlString(modId)}]]
            modId = ${tomlString(it)}
            type = "required"
            versionRange = "*"
            ordering = "AFTER"
            side = "BOTH"
            """.trimIndent()
        }
    val base =
        """
        modLoader = "javafml"
        loaderVersion = ${tomlString(javaFmlLoaderVersionRange)}
        license = ${tomlString(license)}

        [[mods]]
        modId = ${tomlString(modId)}
        version = "1"
        displayName = ${tomlString(displayName)}
        description = ${tomlString(description)}

        [[dependencies.${tomlString(modId)}]]
        modId = "neoforge"
        type = "required"
        versionRange = ${tomlString(neoForgeVersionRange)}
        ordering = "NONE"
        side = "BOTH"

        [[dependencies.${tomlString(modId)}]]
        modId = "minecraft"
        type = "required"
        versionRange = ${tomlString(minecraftVersionRange)}
        ordering = "NONE"
        side = "BOTH"
        """.trimIndent()
    return listOfNotNull(base, testedModDependency).joinToString("\n\n", postfix = "\n")
}

internal fun tomlString(value: String): String =
    buildString {
        append('"')
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
        append('"')
    }
