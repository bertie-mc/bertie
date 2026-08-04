package io.github.bertie_mc.gradle

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler

fun DependencyHandler.packRuntime(vararg notations: Any) {
    addAll("packRuntime", notations)
}

private fun DependencyHandler.addAll(configuration: String, notations: Array<out Any>) {
    notations.forEach { notation -> add(configuration, notation) }
}

/**
 * Treats each declared dependency as one pack payload artifact, rather than inheriting its
 * published dependency graph. This applies equally to external modules and owned projects.
 */
internal fun Configuration.useDirectArtifactsOnly() {
    dependencies.withType(ModuleDependency::class.java).configureEach {
        isTransitive = false
    }
}
