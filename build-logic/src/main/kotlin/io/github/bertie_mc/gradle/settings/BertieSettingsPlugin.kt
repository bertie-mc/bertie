package io.github.bertie_mc.gradle.settings

import io.github.bertie_mc.gradle.model.parseMinecraftArtifacts
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

private const val MINECRAFT_ARTIFACTS_PATH = "gradle/minecraft-artifacts.toml"

class BertieSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.pluginManager.apply("net.neoforged.moddev.repositories")

        val manifestFile = settings.layout.settingsDirectory.file(MINECRAFT_ARTIFACTS_PATH)
        val manifest = parseMinecraftArtifacts(
            settings.providers.fileContents(manifestFile).asText.get(),
        )
        settings.dependencyResolutionManagement.versionCatalogs.create("mods") {
            manifest.mods.forEach { artifact ->
                val source = artifact.gradleSource
                library(artifact.catalogAlias, source.group, source.module).version(source.version)
            }
        }
    }
}
