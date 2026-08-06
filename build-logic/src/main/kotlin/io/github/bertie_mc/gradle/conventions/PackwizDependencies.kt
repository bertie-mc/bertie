package io.github.bertie_mc.gradle.conventions

import io.github.bertie_mc.gradle.model.CurseForgeArtifactSource
import io.github.bertie_mc.gradle.model.MavenArtifactSource
import io.github.bertie_mc.gradle.model.MinecraftArtifact
import io.github.bertie_mc.gradle.model.MinecraftArtifactSource
import io.github.bertie_mc.gradle.model.ModrinthArtifactSource
import io.github.bertie_mc.gradle.model.PackwizArtifact
import io.github.bertie_mc.gradle.model.PackwizProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider

/** Resolves the immutable distribution source without adding it to a Minecraft runtime. */
internal fun Project.packagingClasspath(
    name: String,
    selectedArtifacts: Provider<List<MinecraftArtifact>>,
): Provider<out Configuration> {
    val bucket =
        configurations.dependencyScope(name.removeSuffix("Artifacts")) {
            description = "Packwiz provider artifacts selected from the Minecraft manifest"
            useDirectArtifactsOnly()
            defaultDependencies {
                selectedArtifacts.get().forEach { artifact ->
                    val source = artifact.packwizSource
                    val extension = artifact.kind.extension.takeUnless { it == "jar" }
                    add(
                        this@packagingClasspath.dependencies.create(
                            source.notation(extension),
                        ),
                    )
                }
            }
        }
    return configurations.resolvable(name) {
        description = "Resolved artifacts from ${bucket.get().name}"
        extendsFrom(bucket.get())
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.EXTERNAL))
            attribute(
                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                objects.named(LibraryElements::class.java, LibraryElements.JAR),
            )
        }
    }
}

internal fun Configuration.externalPackwizArtifacts(
    selectedArtifacts: Provider<List<MinecraftArtifact>>,
): Provider<List<PackwizArtifact>> =
    selectedArtifacts.zip(incoming.artifacts.resolvedArtifacts) { artifacts, resolvedArtifacts ->
        val byCoordinate = artifacts.associateBy { artifact -> artifact.packwizSource.coordinate() }
        resolvedArtifacts.map { resolved ->
            val component =
                resolved.id.componentIdentifier as? ModuleComponentIdentifier
                    ?: error("Packaging configuration resolved a non-module artifact: ${resolved.id}")
            val artifact =
                byCoordinate[component.coordinate()]
                    ?: error("Resolved undeclared packaging artifact: $component")
            when (val source = artifact.packwizSource) {
                is ModrinthArtifactSource -> {
                    PackwizArtifact(
                        id = artifact.id,
                        displayName = artifact.id.replace('-', ' '),
                        installedName = source.filename,
                        destination = artifact.kind.destination,
                        side = artifact.side,
                        provider = PackwizProvider.MODRINTH,
                        projectId = source.projectId,
                        versionId = source.versionId,
                        file = resolved.file,
                    )
                }

                is CurseForgeArtifactSource -> {
                    PackwizArtifact(
                        id = artifact.id,
                        displayName = artifact.id.replace('-', ' '),
                        installedName = "${artifact.id}.${resolved.file.extension}",
                        destination = artifact.kind.destination,
                        side = artifact.side,
                        provider = PackwizProvider.CURSEFORGE,
                        projectId = source.projectId.toString(),
                        versionId = source.fileId.toString(),
                        file = resolved.file,
                    )
                }

                is MavenArtifactSource -> {
                    error(
                        "Packwiz cannot package Maven-only artifact '${artifact.id}'",
                    )
                }
            }
        }
    }

internal fun Configuration.ownedPackFiles(): FileCollection =
    incoming
        .artifactView {
            componentFilter { identifier -> identifier is org.gradle.api.artifacts.component.ProjectComponentIdentifier }
        }.files

private fun MinecraftArtifactSource.coordinate(): String = "$group:$module:$version"

private fun ModuleComponentIdentifier.coordinate(): String = "$group:$module:$version"
