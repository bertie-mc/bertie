package io.github.bertie_mc.gradle.conventions

import io.github.bertie_mc.gradle.model.MinecraftArtifact
import io.github.bertie_mc.gradle.model.MinecraftArtifactKind
import io.github.bertie_mc.gradle.model.MinecraftArtifactManifest
import io.github.bertie_mc.gradle.model.MinecraftArtifactSide
import io.github.bertie_mc.gradle.model.MinecraftComponent
import io.github.bertie_mc.gradle.model.MinecraftComponentSelection
import io.github.bertie_mc.gradle.model.ModrinthArtifactSource
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class PackDependenciesTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `pack buckets consume only direct external and project artifacts`() {
        val root = ProjectBuilder.builder().withProjectDir(temporaryDirectory.toFile()).build()
        val ownedModDirectory = Files.createDirectory(temporaryDirectory.resolve("owned-mod"))
        ProjectBuilder
            .builder()
            .withName("owned-mod")
            .withProjectDir(ownedModDirectory.toFile())
            .withParent(root)
            .build()
        val bucket = root.configurations.create("packRuntime")
        val externalDependency =
            root.dependencies.create("example:external-mod:1.0")
                as ModuleDependency
        bucket.dependencies.add(externalDependency)

        bucket.useDirectArtifactsOnly()
        val projectDependency =
            root.dependencies.project(mapOf("path" to ":owned-mod"))
                as ModuleDependency
        bucket.dependencies.add(projectDependency)

        assertFalse(externalDependency.isTransitive)
        assertFalse(projectDependency.isTransitive)
    }

    @Test
    fun `component bucket requests the locked file type for native pack roots`() {
        val project = ProjectBuilder.builder().withProjectDir(temporaryDirectory.toFile()).build()
        val bucket = project.configurations.create("packComponents")
        val dependency =
            project.dependencies.create("maven.modrinth:resourcepack:1")
                as ExternalModuleDependency
        bucket.dependencies.add(dependency)
        val artifact =
            MinecraftArtifact(
                identity = "modrinth:resourcepack:1",
                id = "resourcepack",
                component = "resourcepack",
                kind = MinecraftArtifactKind.RESOURCEPACK,
                side = MinecraftArtifactSide.CLIENT,
                source = ModrinthArtifactSource("resourcepack", "1"),
                filename = "resourcepack.zip",
                provides = emptyList(),
                bundledProvides = emptyList(),
                required = emptyList(),
                optional = emptyList(),
            )
        val manifest =
            MinecraftArtifactManifest(
                profile = "development",
                inputHash = "fixture",
                components =
                    mapOf(
                        "resourcepack" to MinecraftComponent("resourcepack"),
                    ),
                selections =
                    mapOf(
                        "resourcepack" to MinecraftComponentSelection(artifact.identity, null),
                    ),
                artifacts = mapOf(artifact.identity to artifact),
            )

        bucket.useLockedArtifactTypes(manifest)

        assertEquals("zip", dependency.artifacts.single().extension)
        assertEquals("resourcepack", dependency.artifacts.single().name)
    }

    @Test
    fun `packaging provider stays lazy and preserves artifact metadata`() {
        val project = ProjectBuilder.builder().withProjectDir(temporaryDirectory.toFile()).build()
        val repository = Files.createDirectory(temporaryDirectory.resolve("repository"))
        Files.writeString(repository.resolve("artifact-1.zip"), "zip payload")
        project.repositories.flatDir { dirs(repository.toFile()) }
        val evaluations = AtomicInteger()
        val selectedArtifacts =
            project.providers.provider {
                evaluations.incrementAndGet()
                listOf(
                    MinecraftArtifact(
                        identity = "modrinth:artifact:1",
                        id = "example",
                        component = "example",
                        kind = MinecraftArtifactKind.SHADERPACK,
                        side = MinecraftArtifactSide.CLIENT,
                        source =
                            ModrinthArtifactSource(
                                projectId = "artifact",
                                versionId = "1",
                            ),
                        filename = "example-shader.zip",
                        provides = emptyList(),
                        bundledProvides = emptyList(),
                        required = emptyList(),
                        optional = emptyList(),
                    ),
                )
            }
        val packaging =
            project.packagingClasspath(
                "examplePackagingArtifacts",
                selectedArtifacts,
            )

        project.configurations.create("unrelated").resolve()
        assertEquals(0, evaluations.get())

        val files = packaging.get().resolve()
        assertEquals(1, evaluations.get())
        assertEquals(listOf("artifact-1.zip"), files.map { it.name })

        val packaged =
            packaging
                .get()
                .externalPackwizArtifacts(selectedArtifacts)
                .get()
                .single()
        assertEquals(MinecraftArtifactSide.CLIENT, packaged.side)
        assertEquals("shaderpacks", packaged.destination)
    }

    @Test
    fun `packaging emits every installation kind of a native pack`() {
        val project = ProjectBuilder.builder().withProjectDir(temporaryDirectory.toFile()).build()
        val repository = Files.createDirectory(temporaryDirectory.resolve("datapack-repository"))
        Files.writeString(repository.resolve("artifact-1.zip"), "zip payload")
        project.repositories.flatDir { dirs(repository.toFile()) }
        val selectedArtifacts =
            project.providers.provider {
                listOf(
                    MinecraftArtifact(
                        identity = "modrinth:artifact:1",
                        id = "example",
                        component = "example",
                        kind = MinecraftArtifactKind.DATAPACK,
                        side = MinecraftArtifactSide.BOTH,
                        source =
                            ModrinthArtifactSource(
                                projectId = "artifact",
                                versionId = "1",
                            ),
                        filename = "example-datapack.zip",
                        provides = emptyList(),
                        bundledProvides = emptyList(),
                        required = emptyList(),
                        optional = emptyList(),
                        additionalKinds = listOf(MinecraftArtifactKind.RESOURCEPACK),
                    ),
                )
            }
        val packaging =
            project.packagingClasspath(
                "exampleDatapackPackagingArtifacts",
                selectedArtifacts,
            )

        val packaged =
            packaging
                .get()
                .externalPackwizArtifacts(selectedArtifacts)
                .get()

        assertEquals(
            listOf("config/paxi/resourcepacks", "datapacks"),
            packaged.map { artifact -> artifact.destination }.sorted(),
        )
        assertEquals(
            setOf("example-datapack.zip"),
            packaged.map { artifact -> artifact.filename }.toSet(),
        )
    }
}
