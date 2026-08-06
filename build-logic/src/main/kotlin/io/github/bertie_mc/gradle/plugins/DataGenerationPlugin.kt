package io.github.bertie_mc.gradle.plugins

import io.github.bertie_mc.gradle.conventions.addArm64LwjglNatives
import io.github.bertie_mc.gradle.conventions.projectMinecraftRuntime
import io.github.bertie_mc.gradle.model.MinecraftArtifactSide
import io.github.bertie_mc.gradle.model.ModMetadata
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType

class DataGenerationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("bertie.mod") { configure(project) }
    }

    private fun configure(project: Project) =
        with(project) {
            val metadata = extensions.getByType<ModMetadata>()
            val neoForge = extensions.getByType<NeoForgeExtension>()
            val sourceSets = extensions.getByType<SourceSetContainer>()
            val main = sourceSets.getByName("main")
            val datagen = sourceSets.create("datagen")

            configurations.getByName(datagen.implementationConfigurationName).extendsFrom(
                configurations.getByName(main.implementationConfigurationName),
            )
            configurations.getByName(datagen.compileOnlyConfigurationName).extendsFrom(
                configurations.getByName(main.compileOnlyConfigurationName),
            )
            configurations.getByName(datagen.runtimeOnlyConfigurationName).extendsFrom(
                configurations.getByName(main.runtimeOnlyConfigurationName),
            )
            datagen.compileClasspath += main.output
            datagen.runtimeClasspath += main.output
            projectMinecraftRuntime(
                configurations.getByName(datagen.runtimeClasspathConfigurationName),
                MinecraftArtifactSide.CLIENT,
            )
            neoForge.addModdingDependenciesTo(datagen)

            val generatedResources = layout.projectDirectory.dir("src/generated/resources")
            main.resources.srcDir(generatedResources)
            val subjectMod = neoForge.mods.getByName(metadata.id)
            subjectMod.modSourceSets.add(datagen)
            neoForge.runs.register("data") {
                data()
                sourceSet.set(datagen)
                loadedMods.set(setOf(subjectMod))
                gameDirectory.set(layout.buildDirectory.dir("minecraft-runs/datagen"))
                programArguments.addAll(
                    "--mod",
                    metadata.id,
                    "--all",
                    "--output",
                    generatedResources.asFile.absolutePath,
                    "--existing",
                    layout.projectDirectory
                        .dir("src/main/resources")
                        .asFile.absolutePath,
                )
                addArm64LwjglNatives(this)
            }
        }
}
