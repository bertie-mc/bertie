package io.github.bertie_mc.gradle.tasks

import com.google.gson.JsonParser
import io.github.bertie_mc.gradle.model.CurseForgeManifestArtifact
import io.github.bertie_mc.gradle.model.MinecraftArtifactSide
import io.github.bertie_mc.gradle.model.PackwizArtifact
import io.github.bertie_mc.gradle.model.PackwizProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

class GenerateCurseForgePackTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `writes native CurseForge entries and embeds provider fallbacks`() {
        val task = task()
        val fallback = file("artifacts/fallback.jar", "fallback")
        val owned = file("artifacts/owned.jar", "owned")
        task.nativeArtifacts.set(
            listOf(CurseForgeManifestArtifact(123, 456)),
        )
        task.embeddedArtifacts.set(listOf(artifact(fallback)))
        task.localModFiles.from(owned)

        task.generate()

        ZipFile(task.outputFile.get().asFile).use { zip ->
            val manifest =
                JsonParser
                    .parseString(zip.getInputStream(zip.getEntry("manifest.json")).reader().readText())
                    .asJsonObject
            assertEquals("minecraftModpack", manifest.get("manifestType").asString)
            assertEquals(1, manifest.get("manifestVersion").asInt)
            assertEquals("1.21.1", manifest.getAsJsonObject("minecraft").get("version").asString)
            assertEquals(
                "neoforge-21.1.233",
                manifest
                    .getAsJsonObject("minecraft")
                    .getAsJsonArray("modLoaders")
                    .single()
                    .asJsonObject
                    .get("id")
                    .asString,
            )
            val entry = manifest.getAsJsonArray("files").single().asJsonObject
            assertEquals(123, entry.get("projectID").asInt)
            assertEquals(456, entry.get("fileID").asInt)
            assertTrue(entry.get("required").asBoolean)
            assertTrue(zip.getEntry("overrides/mods/fallback.jar") != null)
            assertTrue(zip.getEntry("overrides/mods/owned.jar") != null)
            assertTrue(zip.getEntry("overrides/config/example.toml") != null)
        }
        val audit =
            task.embeddingAuditFile
                .get()
                .asFile
                .readText()
        assertTrue(audit.contains("modrinth:project:version"))
        assertTrue(audit.contains("evidence: MISSING"))
    }

    @Test
    fun `strict redistribution rejects an embedded file without evidence`() {
        val task = task()
        task.embeddedArtifacts.set(listOf(artifact(file("artifacts/fallback.jar", "fallback"))))
        task.redistributionStrict.set(true)

        val failure = assertThrows(IllegalStateException::class.java, task::generate)

        assertTrue(failure.message.orEmpty().contains("modrinth:project:version"))
    }

    @Test
    fun `rejects two native files from the same CurseForge project`() {
        val task = task()
        task.nativeArtifacts.set(
            listOf(
                CurseForgeManifestArtifact(123, 456),
                CurseForgeManifestArtifact(123, 789),
            ),
        )

        val failure = assertThrows(IllegalArgumentException::class.java, task::generate)

        assertTrue(failure.message.orEmpty().contains("123"))
    }

    private fun task(): GenerateCurseForgePack {
        val project = ProjectBuilder.builder().withProjectDir(temporaryDirectory.toFile()).build()
        val properties =
            file(
                "pack.properties",
                "name=Bertie\nauthor=Bertie\nversion=1.0.0\ndescription=Pack\n",
            )
        val config = file("config/example.toml", "example=true\n").parent
        return project.tasks
            .register("generateCurseForgePack", GenerateCurseForgePack::class.java)
            .get()
            .apply {
                packProperties.set(properties.toFile())
                contentDirectory.set(config.toFile())
                minecraftVersion.set("1.21.1")
                neoForgeVersion.set("21.1.233")
                nativeArtifacts.set(emptyList())
                embeddedArtifacts.set(emptyList())
                redistributionStrict.set(false)
                redistributionEvidence.set(emptyList())
                redistributionArtifacts.set(emptyList())
                outputFile.set(temporaryDirectory.resolve("build/bertie-curseforge.zip").toFile())
                embeddingAuditFile.set(temporaryDirectory.resolve("build/audit.txt").toFile())
            }
    }

    private fun artifact(path: Path): PackwizArtifact =
        PackwizArtifact(
            id = "fallback",
            displayName = "fallback",
            filename = "fallback.jar",
            destination = "mods",
            side = MinecraftArtifactSide.BOTH,
            provider = PackwizProvider.MODRINTH,
            projectId = "project",
            versionId = "version",
            file = path.toFile(),
        )

    private fun file(
        relative: String,
        contents: String,
    ): Path =
        temporaryDirectory.resolve(relative).also { path ->
            Files.createDirectories(path.parent)
            Files.writeString(path, contents)
        }
}
