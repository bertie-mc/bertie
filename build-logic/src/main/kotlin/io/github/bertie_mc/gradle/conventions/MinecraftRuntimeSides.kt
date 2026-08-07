package io.github.bertie_mc.gradle.conventions

import io.github.bertie_mc.gradle.model.MinecraftArtifactManifest
import io.github.bertie_mc.gradle.model.MinecraftArtifactSide
import io.github.bertie_mc.gradle.model.loadMinecraftArtifacts
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

internal data class MinecraftModule(
    val group: String,
    val name: String,
)

internal fun MinecraftArtifactManifest.modulesExcludedFrom(target: MinecraftArtifactSide): Set<MinecraftModule> =
    mods
        .asSequence()
        .filterNot { artifact -> artifact.side.isIncludedOn(target) }
        .map { artifact ->
            MinecraftModule(
                group = artifact.gradleSource.group,
                name = artifact.gradleSource.module,
            )
        }.toSet()

internal fun Project.projectMinecraftRuntime(
    runtimeClasspath: Configuration,
    target: MinecraftArtifactSide,
) {
    val manifest =
        loadMinecraftArtifacts(
            layout.settingsDirectory.asFile,
            profile = "development",
        )
    runtimeClasspath.excludeModules(manifest.modulesExcludedFrom(target))
}

private fun Configuration.excludeModules(modules: Set<MinecraftModule>) {
    modules.forEach { module ->
        exclude(
            mapOf(
                "group" to module.group,
                "module" to module.name,
            ),
        )
    }
}
