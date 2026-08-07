package io.github.bertie_mc.gradle.conventions

import io.github.bertie_mc.gradle.model.MinecraftArtifactKind
import io.github.bertie_mc.gradle.model.MinecraftArtifactManifest
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency

/**
 * Treats each declared dependency as one pack payload artifact, rather than inheriting its
 * published dependency graph. This applies equally to external modules and owned projects.
 */
internal fun Configuration.useDirectArtifactsOnly() {
    dependencies.withType(ModuleDependency::class.java).configureEach {
        isTransitive = false
    }
}

/** Requests the actual locked file type for native-pack roots declared through the version catalog. */
internal fun Configuration.useLockedArtifactTypes(manifest: MinecraftArtifactManifest) {
    val artifactsByCoordinate =
        manifest.components.keys.associateBy { component ->
            manifest.selectedArtifact(component).source.coordinate()
        }
    dependencies.withType(ExternalModuleDependency::class.java).configureEach {
        val coordinate = "$group:$name:${versionConstraint.requiredVersion}"
        val locked = artifactsByCoordinate[coordinate]?.let(manifest::selectedArtifact) ?: return@configureEach
        if (locked.kind == MinecraftArtifactKind.MOD) return@configureEach
        artifact {
            name = locked.source.module
            type = locked.kind.extension
            extension = locked.kind.extension
        }
    }
}

private fun io.github.bertie_mc.gradle.model.MinecraftArtifactSource.coordinate(): String = "$group:$module:$version"
