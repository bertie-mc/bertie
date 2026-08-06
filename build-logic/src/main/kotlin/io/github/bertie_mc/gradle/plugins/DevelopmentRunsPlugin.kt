package io.github.bertie_mc.gradle.plugins

import io.github.bertie_mc.gradle.conventions.addArm64LwjglNatives
import io.github.bertie_mc.gradle.conventions.projectMinecraftRuntime
import io.github.bertie_mc.gradle.model.MinecraftArtifactSide
import io.github.bertie_mc.gradle.model.ModMetadata
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

class DevelopmentRunsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("bertie.mod") { configure(project) }
    }

    private fun configure(project: Project) =
        with(project) {
            val metadata = extensions.getByType<ModMetadata>()
            val neoForge = extensions.getByType<NeoForgeExtension>()
            val sourceSets = extensions.getByType<SourceSetContainer>()
            val main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            val subjectMod = neoForge.mods.getByName(metadata.id)

            fun runtimeProjection(
                name: String,
                side: MinecraftArtifactSide,
            ): SourceSet {
                val projection =
                    sourceSets.create("${name}runtime") {
                        java.setSrcDirs(emptyList<String>())
                        resources.setSrcDirs(emptyList<String>())
                    }
                configurations.getByName(projection.implementationConfigurationName).extendsFrom(
                    configurations.getByName(main.implementationConfigurationName),
                )
                configurations.getByName(projection.runtimeOnlyConfigurationName).extendsFrom(
                    configurations.getByName(main.runtimeOnlyConfigurationName),
                )
                projection.runtimeClasspath += main.output
                projectMinecraftRuntime(
                    configurations.getByName(projection.runtimeClasspathConfigurationName),
                    side,
                )
                neoForge.addModdingDependenciesTo(projection)
                return projection
            }

            fun prepareInstance(name: String) =
                tasks.register<Sync>(
                    "prepare${name.replaceFirstChar(Char::uppercaseChar)}Instance",
                ) {
                    into(layout.buildDirectory.dir("minecraft-runs/$name"))
                    from(layout.projectDirectory.dir("src/main/instance"))
                }

            val client = runtimeProjection("client", MinecraftArtifactSide.CLIENT)
            val prepareClient = prepareInstance("client")
            neoForge.runs.register("client") {
                client()
                sourceSet.set(client)
                loadedMods.set(setOf(subjectMod))
                gameDirectory.set(layout.buildDirectory.dir("minecraft-runs/client"))
                taskBefore(prepareClient)
                addArm64LwjglNatives(this)
            }

            val server = runtimeProjection("server", MinecraftArtifactSide.SERVER)
            val prepareServer = prepareInstance("server")
            neoForge.runs.register("server") {
                server()
                sourceSet.set(server)
                loadedMods.set(setOf(subjectMod))
                gameDirectory.set(layout.buildDirectory.dir("minecraft-runs/server"))
                programArgument("--nogui")
                taskBefore(prepareServer)
            }
        }
}
