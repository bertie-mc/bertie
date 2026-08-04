package io.github.bertie_mc.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet

internal data class MinecraftModule(
    val group: String,
    val name: String,
)

internal fun MinecraftArtifactManifest.modulesExcludedFrom(
    target: MinecraftArtifactSide,
): Set<MinecraftModule> {
    return mods.asSequence()
        .filterNot { artifact -> artifact.side.isIncludedOn(target) }
        .map { artifact ->
            MinecraftModule(
                group = artifact.gradleSource.group,
                name = artifact.gradleSource.module,
            )
        }
        .toSet()
}

/**
 * Projects a source set's runtime onto one physical Minecraft distribution.
 *
 * Only manifest-owned third-party modules are excluded. Project dependencies,
 * ordinary Java libraries, and the source set's compile classpath are untouched.
 */
fun Project.projectMinecraftRuntime(
    sourceSet: SourceSet,
    target: MinecraftArtifactSide,
) {
    val manifest = parseMinecraftArtifacts(
        providers.fileContents(
            rootProject.layout.projectDirectory.file("gradle/minecraft-artifacts.toml"),
        ).asText.get(),
    )
    val runtimeClasspath = configurations.getByName(
        sourceSet.runtimeClasspathConfigurationName,
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
