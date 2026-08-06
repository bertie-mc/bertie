package io.github.bertie_mc.gradle.tasks

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class GenerateSuiteModMetadataTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `writes conventional NeoForge metadata`() {
        val project = ProjectBuilder.builder().withProjectDir(temporaryDirectory.toFile()).build()
        val task =
            project.tasks
                .register(
                    "generateSuiteMetadata",
                    GenerateSuiteModMetadata::class.java,
                ).get()
        task.modId.set("example_gametests")
        task.displayName.set("Example GameTests")
        task.descriptionText.set("GameTests for Example")
        task.license.set("Unlicense")
        task.minecraftVersionRange.set("[1.20.4,1.21)")
        task.neoForgeVersionRange.set("[20.4,)")
        task.javaFmlLoaderVersionRange.set("[3,)")
        task.testedModId.set("example")
        task.outputDirectory.set(temporaryDirectory.resolve("generated").toFile())

        task.generate()

        assertEquals(
            """
            modLoader = "javafml"
            loaderVersion = "[3,)"
            license = "Unlicense"

            [[mods]]
            modId = "example_gametests"
            version = "1"
            displayName = "Example GameTests"
            description = "GameTests for Example"

            [[dependencies."example_gametests"]]
            modId = "neoforge"
            type = "required"
            versionRange = "[20.4,)"
            ordering = "NONE"
            side = "BOTH"

            [[dependencies."example_gametests"]]
            modId = "minecraft"
            type = "required"
            versionRange = "[1.20.4,1.21)"
            ordering = "NONE"
            side = "BOTH"

            [[dependencies."example_gametests"]]
            modId = "example"
            type = "required"
            versionRange = "*"
            ordering = "AFTER"
            side = "BOTH"
            """.trimIndent() + "\n",
            temporaryDirectory
                .resolve("generated/META-INF/neoforge.mods.toml")
                .toFile()
                .readText(),
        )
    }

    @Test
    fun `escapes TOML basic strings`() {
        assertEquals(
            "\"quote \\\" slash \\\\ line\\nreturn\\rtab\\t\"",
            tomlString("quote \" slash \\ line\nreturn\rtab\t"),
        )
    }
}
