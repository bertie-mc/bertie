package io.github.bertie_mc.gradle.plugins

import io.github.bertie_mc.gradle.conventions.configureMinecraftOperatingSystemOverride
import io.github.bertie_mc.gradle.conventions.requiredVersion
import io.github.bertie_mc.gradle.model.loadPlatformVersions
import io.github.bertie_mc.gradle.tasks.DownloadMinecraftArtifacts
import io.github.bertie_mc.gradle.tasks.ResolveDependencies
import net.neoforged.minecraftdependencies.MinecraftDependenciesPlugin
import net.neoforged.minecraftdependencies.MinecraftDistribution
import net.neoforged.nfrtgradle.DownloadAssets
import net.neoforged.nfrtgradle.NeoFormRuntimeExtension
import net.neoforged.nfrtgradle.NeoFormRuntimePlugin
import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JvmEcosystemPlugin
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

class MinecraftInputsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            pluginManager.apply(BasePlugin::class.java)
            pluginManager.apply(JvmEcosystemPlugin::class.java)
            pluginManager.apply(MinecraftDependenciesPlugin::class.java)
            pluginManager.apply(NeoFormRuntimePlugin::class.java)
            dependencyLocking.lockAllConfigurations()
            configureMinecraftOperatingSystemOverride()

            val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
            val minecraftVersion =
                loadPlatformVersions(layout.settingsDirectory.asFile, catalog).minecraft
            extensions.getByType<NeoFormRuntimeExtension>().version.set(
                catalog.requiredVersion("neoform-runtime"),
            )

            val clientDependencies =
                configurations.dependencyScope("minecraftClientDependencies")
            val client =
                configurations
                    .resolvable("minecraftClient") {
                        description = "Minecraft client libraries and native artifacts"
                        extendsFrom(clientDependencies.get())
                        attributes {
                            named(this@with, Category.CATEGORY_ATTRIBUTE, Category.LIBRARY)
                            named(this@with, Usage.USAGE_ATTRIBUTE, Usage.JAVA_RUNTIME)
                            named(
                                this@with,
                                MinecraftDistribution.ATTRIBUTE,
                                MinecraftDistribution.CLIENT,
                            )
                        }
                    }.get()
            dependencies.add(
                clientDependencies.name,
                "net.neoforged:minecraft-dependencies:$minecraftVersion",
            )

            val resolveInputs =
                tasks.register<ResolveDependencies>("resolveMinecraftInputs") {
                    dependencies.from(client)
                    dependencies.from(configurations.named("neoFormRuntimeTool"))
                    dependencies.from(configurations.named("neoFormRuntimeExternalTools"))
                }
            val downloadArtifacts =
                tasks.register<DownloadMinecraftArtifacts>(
                    "downloadMinecraftArtifacts",
                ) {
                    this.minecraftVersion.set(minecraftVersion)
                    outputDirectory.set(layout.buildDirectory.dir("minecraft/downloads"))
                }
            val downloadAssets =
                tasks.register<DownloadAssets>("downloadAssets") {
                    this.minecraftVersion.set(minecraftVersion)
                    assetPropertiesFile.set(
                        layout.buildDirectory.file("minecraft/assets/minecraft-assets.properties"),
                    )
                }

            tasks.register("prepareOfflineBuild") {
                group = "build setup"
                description = "Downloads Minecraft inputs and platform seeds for offline jobs"
                dependsOn(resolveInputs, downloadArtifacts, downloadAssets)
            }
            tasks.register("resolveAndLockAll") {
                group = "build setup"
                description = "Resolves Minecraft input dependencies and writes lock state"
                dependsOn(resolveInputs)
            }
        }
    }

    private fun <T : Named> AttributeContainer.named(
        project: Project,
        attribute: Attribute<T>,
        value: String,
    ) {
        attribute(attribute, project.objects.named(attribute.type, value))
    }
}
