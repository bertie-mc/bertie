package io.github.bertie_mc.gradle.settings

import io.github.bertie_mc.gradle.model.MinecraftArtifactKind
import io.github.bertie_mc.gradle.model.loadMinecraftArtifacts
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class BertieSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.pluginManager.apply("net.neoforged.moddev.repositories")

        val manifest =
            loadMinecraftArtifacts(
                settings.layout.settingsDirectory.asFile,
                profile = "development",
            )
        settings.dependencyResolutionManagement.versionCatalogs.create("deps") {
            manifest.components.values.sortedBy { it.id }.forEach { component ->
                val artifact = manifest.selectedArtifact(component.id)
                val source = artifact.source
                library(component.catalogAlias, source.group, source.module).version(source.version)
            }
        }
        settings.dependencyResolutionManagement.components {
            normalizeMinecraftDependencyMetadata()
            manifest.artifacts.values.forEach { artifact ->
                val source = artifact.source
                withModule("${source.group}:${source.module}") {
                    if (id.version == source.version) {
                        allVariants {
                            withDependencies {
                                removeAll { true }
                                artifact.required.forEach { edge ->
                                    val target = manifest.artifacts.getValue(edge.artifact!!)
                                    add(target.notation())
                                }
                            }
                            if (artifact.kind != MinecraftArtifactKind.MOD) {
                                withFiles {
                                    removeAllFiles()
                                    addFile("${source.module}-${source.version}.${artifact.kind.extension}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
