package io.github.bertie_mc.gradle.tasks

import io.github.bertie_mc.gradle.MinecraftArtifactSide
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

class GeneratePackwizPackTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `owned mod filenames participate in the task fingerprint`() {
        val annotation = GeneratePackwizPack::class.java
            .getMethod("getLocalModFiles")
            .getAnnotation(PathSensitive::class.java)

        assertEquals(PathSensitivity.NAME_ONLY, annotation.value)
    }

    @Test
    fun `generates external provider metadata and embeds locally built mods`() {
        val fixture = fixture("providers")
        val modrinthFile = fixture.file("artifacts/modrinth.jar", "modrinth artifact")
        val modrinthZip = fixture.file("artifacts/modrinth.zip", "modrinth zip")
        val curseForgeFile = fixture.file("artifacts/curseforge.jar", "curseforge artifact")
        val ownedFile = fixture.file("artifacts/owned-1.2.3.jar", "owned artifact")
        val modrinthHash = sha512(modrinthFile)
        fixture.task.packArtifacts.set(
            listOf(
                artifact(
                    id = "modrinth-example",
                    file = modrinthFile,
                    provider = PackwizProvider.MODRINTH,
                    projectId = "project-id",
                    versionId = "version-id",
                    installedName = "example-release.jar",
                    side = MinecraftArtifactSide.BOTH,
                ),
                artifact(
                    id = "modrinth-shader",
                    file = modrinthZip,
                    provider = PackwizProvider.MODRINTH,
                    projectId = "shader-project",
                    versionId = "shader-version",
                    destination = "shaderpacks",
                    installedName = "shader-release.zip",
                    side = MinecraftArtifactSide.CLIENT,
                ),
                artifact(
                    id = "curseforge-example",
                    file = curseForgeFile,
                    provider = PackwizProvider.CURSEFORGE,
                    projectId = "123",
                    versionId = "456",
                    side = MinecraftArtifactSide.SERVER,
                ),
            ),
        )
        fixture.task.localModFiles.from(ownedFile)

        fixture.task.generate()
        val firstOutput = snapshot(fixture.output)
        fixture.task.generate()

        assertEquals(firstOutput, snapshot(fixture.output))
        assertEquals(2L, Files.walk(fixture.output.resolve("config")).use { paths ->
            paths.filter(Files::isRegularFile).count()
        })
        assertFalse(Files.exists(fixture.output.resolve("file-0.toml")))

        val modrinth = fixture.generated("mods/modrinth-example.pw.toml")
        assertTrue(modrinth.contains("filename = \"example-release.jar\""))
        assertTrue(
            modrinth.contains(
                "url = \"https://cdn.modrinth.com/data/project-id/versions/" +
                    "version-id/example-release.jar\"",
            ),
        )
        assertTrue(modrinth.contains("hash = \"$modrinthHash\""))
        assertTrue(modrinth.contains("mod-id = \"project-id\""))
        assertTrue(modrinth.contains("version = \"version-id\""))
        assertTrue(modrinth.contains("side = \"both\""))

        val modrinthShader = fixture.generated("shaderpacks/modrinth-shader.pw.toml")
        assertTrue(modrinthShader.contains("filename = \"shader-release.zip\""))
        assertTrue(
            modrinthShader.contains(
                "url = \"https://cdn.modrinth.com/data/shader-project/versions/" +
                    "shader-version/shader-release.zip\"",
            ),
        )
        assertTrue(modrinthShader.contains("side = \"client\""))

        val curseForge = fixture.generated("mods/curseforge-example.pw.toml")
        assertTrue(curseForge.contains("mode = \"metadata:curseforge\""))
        assertTrue(curseForge.contains("project-id = 123"))
        assertTrue(curseForge.contains("file-id = 456"))
        assertTrue(curseForge.contains("side = \"server\""))

        val copiedOwnedMod = fixture.output.resolve("mods/owned-1.2.3.jar")
        assertEquals("owned artifact", Files.readString(copiedOwnedMod))
        assertFalse(Files.exists(fixture.output.resolve("mods/owned-example.pw.toml")))
        val ownedIndexEntry = fixture.generated("index.toml")
            .split("[[files]]")
            .single { entry -> entry.contains("file = \"mods/owned-1.2.3.jar\"") }
        assertFalse(ownedIndexEntry.contains("metafile = true"))

        assertTrue(fixture.generated("pack.toml").contains("version = \"0.1.0\""))
        firstOutput.values.forEach { contents ->
            assertFalse(contents.contains("api.modrinth.com/maven"))
            assertFalse(contents.contains("github.com/bertie-mc/bertie/releases"))
        }
    }

    @Test
    fun `allows external artifact ids to repeat across destinations`() {
        val fixture = fixture("cross-destination-ids")
        val mod = fixture.file("artifacts/mod.jar", "mod")
        val shaderpack = fixture.file("artifacts/shaderpack.zip", "shaderpack")
        fixture.task.packArtifacts.set(
            listOf(
                artifact(
                    id = "example",
                    file = mod,
                    provider = PackwizProvider.MODRINTH,
                    projectId = "mod-project",
                    versionId = "mod-version",
                ),
                artifact(
                    id = "example",
                    file = shaderpack,
                    provider = PackwizProvider.MODRINTH,
                    projectId = "shader-project",
                    versionId = "shader-version",
                    destination = "shaderpacks",
                ),
            ),
        )

        fixture.task.generate()

        assertTrue(Files.isRegularFile(fixture.output.resolve("mods/example.pw.toml")))
        assertTrue(Files.isRegularFile(fixture.output.resolve("shaderpacks/example.pw.toml")))
    }

    @Test
    fun `rejects duplicate external metafile paths`() {
        val fixture = fixture("collisions")
        val first = fixture.file("artifacts/first.jar", "first")
        val second = fixture.file("artifacts/second.jar", "second")
        fixture.task.packArtifacts.set(
            listOf(
                artifact(
                    id = "example",
                    file = first,
                    provider = PackwizProvider.MODRINTH,
                    projectId = "project",
                    versionId = "first",
                ),
                artifact(
                    id = "example",
                    file = second,
                    provider = PackwizProvider.MODRINTH,
                    projectId = "project",
                    versionId = "second",
                ),
            ),
        )

        val failure = assertThrows(IllegalArgumentException::class.java) {
            fixture.task.generate()
        }

        assertTrue(failure.message.orEmpty().contains("metafile paths collide"))
    }

    private fun fixture(name: String): Fixture {
        val projectDirectory = Files.createDirectories(temporaryDirectory.resolve(name))
        val project = ProjectBuilder.builder().withProjectDir(projectDirectory.toFile()).build()
        val properties = projectDirectory.resolve("pack.properties")
        Files.writeString(
            properties,
            "name=Bertie\nauthor=Bertie contributors\nversion=0.1.0\n" +
                "description=Integration pack\n",
        )
        val config = projectDirectory.resolve("config")
        repeat(2) { index ->
            val file = config.resolve("nested/file-$index.toml")
            Files.createDirectories(file.parent)
            Files.writeString(file, "value = $index\n")
        }
        val output = projectDirectory.resolve("build/packwiz")
        val task = project.tasks.register("generatePackwiz", GeneratePackwizPack::class.java).get()
        task.packProperties.set(properties.toFile())
        task.contentDirectory.set(config.toFile())
        task.minecraftVersion.set("1.21.1")
        task.neoForgeVersion.set("21.1.233")
        task.outputDirectory.set(output.toFile())
        return Fixture(projectDirectory, output, task)
    }

    private fun artifact(
        id: String,
        file: Path,
        provider: PackwizProvider,
        projectId: String,
        versionId: String,
        destination: String = "mods",
        installedName: String = "$id.${file.fileName.toString().substringAfterLast('.')}",
        side: MinecraftArtifactSide = MinecraftArtifactSide.BOTH,
    ): PackwizArtifact = PackwizArtifact(
        id = id,
        displayName = id,
        installedName = installedName,
        destination = destination,
        side = side,
        provider = provider,
        projectId = projectId,
        versionId = versionId,
        file = file.toFile(),
    )

    private fun sha512(path: Path): String =
        MessageDigest.getInstance("SHA-512").digest(Files.readAllBytes(path))
            .joinToString("") { byte -> "%02x".format(byte) }

    private fun snapshot(root: Path): Map<String, String> = Files.walk(root).use { paths ->
        paths.filter(Files::isRegularFile)
            .sorted()
            .toList()
            .associate { path -> root.relativize(path).toString() to Files.readString(path) }
    }

    private data class Fixture(
        val root: Path,
        val output: Path,
        val task: GeneratePackwizPack,
    ) {
        fun file(relative: String, contents: String): Path = root.resolve(relative).also { path ->
            Files.createDirectories(path.parent)
            Files.writeString(path, contents)
        }

        fun generated(relative: String): String = Files.readString(output.resolve(relative))
    }
}
