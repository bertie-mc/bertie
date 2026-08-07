package io.github.bertie_mc.gradle.tasks

import com.google.gson.JsonParser
import io.github.bertie_mc.gradle.model.MinecraftArtifactSide
import io.github.bertie_mc.gradle.model.PackwizArtifact
import io.github.bertie_mc.gradle.model.PackwizProvider
import io.github.bertie_mc.gradle.model.RedistributionArtifactPolicy
import io.github.bertie_mc.gradle.model.RedistributionEvidence
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

class GenerateMrpackTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `writes native Modrinth entries and embeds non-native files with an audit`() {
        val task = task()
        val modrinth = file("artifacts/modrinth.jar", "modrinth")
        val curseForge = file("artifacts/curseforge.jar", "curseforge")
        val owned = file("artifacts/owned.jar", "owned")
        task.externalArtifacts.set(
            listOf(
                artifact(
                    "native",
                    modrinth,
                    PackwizProvider.MODRINTH,
                    "project",
                    "version",
                    MinecraftArtifactSide.CLIENT,
                ),
                artifact(
                    "embedded",
                    curseForge,
                    PackwizProvider.CURSEFORGE,
                    "12",
                    "34",
                    MinecraftArtifactSide.BOTH,
                ),
            ),
        )
        task.localModFiles.from(owned)

        task.generate()

        ZipFile(task.outputFile.get().asFile).use { zip ->
            val index =
                JsonParser
                    .parseString(
                        zip
                            .getInputStream(zip.getEntry("modrinth.index.json"))
                            .reader()
                            .readText(),
                    ).asJsonObject
            val entry = index.getAsJsonArray("files").single().asJsonObject
            assertEquals("mods/native.jar", entry.get("path").asString)
            assertEquals("required", entry.getAsJsonObject("env").get("client").asString)
            assertEquals("unsupported", entry.getAsJsonObject("env").get("server").asString)
            assertTrue(zip.getEntry("overrides/mods/embedded.jar") != null)
            assertTrue(zip.getEntry("overrides/mods/owned.jar") != null)
            assertTrue(zip.getEntry("overrides/config/example.toml") != null)
        }
        val audit =
            task.embeddingAuditFile
                .get()
                .asFile
                .readText()
        assertTrue(audit.contains("curseforge:12:34"))
        assertTrue(audit.contains("evidence: MISSING"))
    }

    @Test
    fun `strict redistribution rejects an embedded file without evidence`() {
        val task = task()
        val curseForge = file("artifacts/curseforge.jar", "curseforge")
        task.externalArtifacts.set(
            listOf(
                artifact(
                    "embedded",
                    curseForge,
                    PackwizProvider.CURSEFORGE,
                    "12",
                    "34",
                    MinecraftArtifactSide.BOTH,
                ),
            ),
        )
        task.redistributionStrict.set(true)

        val failure = assertThrows(IllegalStateException::class.java, task::generate)

        assertTrue(failure.message.orEmpty().contains("curseforge:12:34"))
    }

    @Test
    fun `one named evidence record covers multiple embedded files`() {
        val task = task()
        val first = file("artifacts/first.jar", "first")
        val second = file("artifacts/second.jar", "second")
        task.externalArtifacts.set(
            listOf(
                artifact(
                    "first",
                    first,
                    PackwizProvider.CURSEFORGE,
                    "12",
                    "34",
                    MinecraftArtifactSide.BOTH,
                ),
                artifact(
                    "second",
                    second,
                    PackwizProvider.CURSEFORGE,
                    "56",
                    "78",
                    MinecraftArtifactSide.BOTH,
                ),
            ),
        )
        task.redistributionStrict.set(true)
        task.redistributionEvidence.set(
            listOf(
                RedistributionEvidence(
                    id = "shared-permission",
                    allowed = true,
                    text = "The author permits redistribution.",
                ),
            ),
        )
        task.redistributionArtifacts.set(
            listOf(
                RedistributionArtifactPolicy(
                    identity = "curseforge:12:34",
                    name = "first",
                    component = null,
                    evidence = listOf("shared-permission"),
                ),
                RedistributionArtifactPolicy(
                    identity = "curseforge:56:78",
                    name = "second",
                    component = null,
                    evidence = listOf("shared-permission"),
                ),
            ),
        )

        task.generate()

        val audit =
            task.embeddingAuditFile
                .get()
                .asFile
                .readText()
        assertEquals(2, Regex("\\[shared-permission]").findAll(audit).count())
        assertEquals(2, Regex("redistribution: ALLOWED").findAll(audit).count())
        assertTrue(audit.contains("The author permits redistribution."))
    }

    @Test
    fun `denied evidence wins conflicts and is rejected only in strict mode`() {
        val task = task()
        val artifact =
            artifact(
                "embedded",
                file("artifacts/embedded.jar", "embedded"),
                PackwizProvider.CURSEFORGE,
                "12",
                "34",
                MinecraftArtifactSide.BOTH,
            )
        task.externalArtifacts.set(listOf(artifact))
        task.redistributionEvidence.set(
            listOf(
                RedistributionEvidence(
                    id = "general-permission",
                    allowed = true,
                    text = "The project generally allows modpack use.",
                ),
                RedistributionEvidence(
                    id = "provider-only",
                    allowed = false,
                    text = "The author permits distribution only through provider manifests.",
                ),
            ),
        )
        task.redistributionArtifacts.set(
            listOf(
                RedistributionArtifactPolicy(
                    identity = "curseforge:12:34",
                    name = "embedded",
                    component = null,
                    evidence = listOf("general-permission", "provider-only"),
                ),
            ),
        )

        task.generate()
        assertTrue(
            task.embeddingAuditFile
                .get()
                .asFile
                .readText()
                .contains("redistribution: DENIED"),
        )

        task.redistributionStrict.set(true)
        val failure = assertThrows(IllegalStateException::class.java, task::generate)
        assertTrue(failure.message.orEmpty().contains("redistribution is denied"))
        assertTrue(failure.message.orEmpty().contains("curseforge:12:34"))
    }

    @Test
    fun `redistribution assignment name must match the locked artifact`() {
        val task = task()
        val artifact =
            artifact(
                "embedded",
                file("artifacts/embedded.jar", "embedded"),
                PackwizProvider.CURSEFORGE,
                "12",
                "34",
                MinecraftArtifactSide.BOTH,
            )
        task.externalArtifacts.set(listOf(artifact))
        task.redistributionEvidence.set(
            listOf(RedistributionEvidence("permission", true, "Redistribution is allowed.")),
        )
        task.redistributionArtifacts.set(
            listOf(
                RedistributionArtifactPolicy(
                    identity = "curseforge:12:34",
                    name = "wrong-name",
                    component = null,
                    evidence = listOf("permission"),
                ),
            ),
        )

        val failure = assertThrows(IllegalArgumentException::class.java, task::generate)

        assertTrue(failure.message.orEmpty().contains("does not match curseforge:12:34"))
    }

    private fun task(): GenerateMrpack {
        val project = ProjectBuilder.builder().withProjectDir(temporaryDirectory.toFile()).build()
        val properties = file("pack.properties", "name=Bertie\nauthor=Bertie\nversion=1.0.0\ndescription=Pack\n")
        val config = file("config/example.toml", "example=true\n").parent
        return project.tasks.register("generateMrpack", GenerateMrpack::class.java).get().apply {
            packProperties.set(properties.toFile())
            contentDirectory.set(config.toFile())
            minecraftVersion.set("1.21.1")
            neoForgeVersion.set("21.1.233")
            redistributionStrict.set(false)
            redistributionEvidence.set(emptyList())
            redistributionArtifacts.set(emptyList())
            outputFile.set(temporaryDirectory.resolve("build/bertie.mrpack").toFile())
            embeddingAuditFile.set(temporaryDirectory.resolve("build/audit.txt").toFile())
        }
    }

    private fun artifact(
        id: String,
        path: Path,
        provider: PackwizProvider,
        projectId: String,
        versionId: String,
        side: MinecraftArtifactSide,
    ): PackwizArtifact =
        PackwizArtifact(
            id = id,
            displayName = id,
            filename = "$id.jar",
            destination = "mods",
            side = side,
            provider = provider,
            projectId = projectId,
            versionId = versionId,
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
