package io.github.bertie_mc.gradle.conventions

import org.gradle.api.artifacts.Configuration
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
