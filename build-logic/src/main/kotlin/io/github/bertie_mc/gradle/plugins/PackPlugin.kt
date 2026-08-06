package io.github.bertie_mc.gradle.plugins

import io.github.bertie_mc.gradle.conventions.configureJvmRole
import io.github.bertie_mc.gradle.conventions.configureNeoForgeRole
import io.github.bertie_mc.gradle.conventions.externalPackwizArtifacts
import io.github.bertie_mc.gradle.conventions.ownedPackFiles
import io.github.bertie_mc.gradle.conventions.packagingClasspath
import io.github.bertie_mc.gradle.conventions.useDirectArtifactsOnly
import io.github.bertie_mc.gradle.model.PlatformVersions
import io.github.bertie_mc.gradle.model.TestSubject
import io.github.bertie_mc.gradle.model.parseMinecraftArtifacts
import io.github.bertie_mc.gradle.tasks.GeneratePackwizPack
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import java.time.Duration

class PackPlugin : Plugin<Project> {
    override fun apply(project: Project) =
        with(project) {
            configureJvmRole()
            val neoForge = configureNeoForgeRole()

            extensions.add(
                TestSubject::class.java,
                "bertieTestSubject",
                TestSubject(
                    id = "bertiepacktests",
                    license = "Unlicense",
                    gameTestNamespace = "bertiepacktests",
                    testedModId = null,
                ),
            )

            tasks.named<Jar>("jar") { enabled = false }

            val manifest =
                providers
                    .fileContents(
                        layout.settingsDirectory.file("gradle/minecraft-artifacts.toml"),
                    ).asText
                    .map(::parseMinecraftArtifacts)
            val externalMods = manifest.map { it.mods }
            val shaderpacks = manifest.map { it.shaderpacks }

            val packMods =
                configurations.dependencyScope("packMods") {
                    description = "Complete third-party and owned mod inventory"
                    useDirectArtifactsOnly()
                }
            val packShaderpacks =
                configurations.dependencyScope("packShaderpacks") {
                    description = "Shaderpacks included in the generated pack"
                    useDirectArtifactsOnly()
                }
            val gameTestRuntimeMods =
                configurations.dependencyScope("gametestRuntimeMods") {
                    extendsFrom(packMods.get())
                }
            val clientTestRuntimeMods =
                configurations.dependencyScope("clienttestRuntimeMods") {
                    extendsFrom(packMods.get())
                }

            externalMods.get().forEach { artifact ->
                dependencies.add(packMods.name, artifact.gradleSource.notation())
            }
            shaderpacks.get().forEach { artifact ->
                dependencies.add(
                    packShaderpacks.name,
                    artifact.gradleSource.notation(artifact.kind.extension),
                )
            }

            val modArtifacts = artifactClasspath("packModArtifacts", packMods)
            val shaderpackArtifacts = artifactClasspath("packShaderpackArtifacts", packShaderpacks)
            val modPackagingArtifacts = packagingClasspath("packModPackagingArtifacts", externalMods)
            val shaderpackPackagingArtifacts =
                packagingClasspath(
                    "packShaderpackPackagingArtifacts",
                    shaderpacks,
                )

            val platform = extensions.getByType<PlatformVersions>()
            tasks.register<GeneratePackwizPack>("generatePackwiz") {
                group = "distribution"
                description = "Generates the packwiz pack from the declared runtime inventory"
                packProperties.set(layout.projectDirectory.file("pack.properties"))
                contentDirectory.set(layout.projectDirectory.dir("config"))
                minecraftVersion.set(platform.minecraft)
                neoForgeVersion.set(platform.neoForge)
                outputDirectory.set(layout.buildDirectory.dir("packwiz"))
                packArtifacts.addAll(
                    modPackagingArtifacts.get().externalPackwizArtifacts(externalMods),
                )
                packArtifacts.addAll(
                    shaderpackPackagingArtifacts.get().externalPackwizArtifacts(shaderpacks),
                )
                localModFiles.from(modArtifacts.ownedPackFiles())
            }

            pluginManager.withPlugin("bertie.gametest") {
                configurations.named("gametestCompileOnly") {
                    extendsFrom(packMods.get())
                }
                configurations.named("gametestRuntimeOnly") {
                    extendsFrom(gameTestRuntimeMods.get())
                }
                tasks.named<Sync>("prepareGameTestInstance") {
                    from(layout.projectDirectory.dir("config")) { into("config") }
                }
                tasks.named("runGameTests") {
                    timeout.set(Duration.ofMinutes(75))
                }
                neoForge.runs.named("gameTests") {
                    jvmArgument("-Xmx8G")
                }
            }
            pluginManager.withPlugin("bertie.client-test") {
                configurations.named("clienttestCompileOnly") {
                    extendsFrom(packMods.get())
                }
                configurations.named("clienttestRuntimeOnly") {
                    extendsFrom(clientTestRuntimeMods.get())
                }
                tasks.named<Sync>("prepareClientTestInstance") {
                    from(layout.projectDirectory.dir("config")) { into("config") }
                    from(shaderpackArtifacts) { into("shaderpacks") }
                }
                tasks.named("runClientTests") {
                    timeout.set(Duration.ofMinutes(105))
                }
                neoForge.runs.named("clientTests") {
                    jvmArgument("-Xmx10G")
                }
            }
        }

    private fun Project.artifactClasspath(
        name: String,
        bucket: Provider<out Configuration>,
    ): Configuration =
        configurations
            .resolvable(name) {
                description = "Direct artifacts from ${bucket.get().name}"
                extendsFrom(bucket.get())
                attributes {
                    attribute(
                        Category.CATEGORY_ATTRIBUTE,
                        objects.named(Category::class.java, Category.LIBRARY),
                    )
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
                    attribute(
                        Bundling.BUNDLING_ATTRIBUTE,
                        objects.named(Bundling::class.java, Bundling.EXTERNAL),
                    )
                    attribute(
                        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        objects.named(LibraryElements::class.java, LibraryElements.JAR),
                    )
                }
            }.get()
}
