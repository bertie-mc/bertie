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

class NeoForgeTestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("bertie.mod") { configure(project) }
    }

    private fun configure(project: Project) =
        with(project) {
            val metadata = extensions.getByType<ModMetadata>()
            val neoForge = extensions.getByType<NeoForgeExtension>()
            val subjectMod = neoForge.mods.getByName(metadata.id)
            neoForge.unitTest {
                enable()
                testedMod.set(subjectMod)
                loadedMods.set(setOf(subjectMod))
            }

            val test = extensions.getByType<SourceSetContainer>().getByName("test")
            projectMinecraftRuntime(
                configurations.getByName(test.runtimeClasspathConfigurationName),
                MinecraftArtifactSide.CLIENT,
            )
            addArm64LwjglNatives(configurations.getByName(test.runtimeOnlyConfigurationName))
        }
}
