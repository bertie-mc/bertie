package io.github.bertie_mc.gradle.plugins

import io.github.bertie_mc.gradle.conventions.configureJvmRole
import io.github.bertie_mc.gradle.conventions.configureNeoForgeRole
import io.github.bertie_mc.gradle.conventions.externalPackwizArtifacts
import io.github.bertie_mc.gradle.conventions.ownedPackFiles
import io.github.bertie_mc.gradle.conventions.packagingClasspath
import io.github.bertie_mc.gradle.conventions.useDirectArtifactsOnly
import io.github.bertie_mc.gradle.model.MinecraftArtifact
import io.github.bertie_mc.gradle.model.MinecraftArtifactSide
import io.github.bertie_mc.gradle.model.PlatformVersions
import io.github.bertie_mc.gradle.model.TestSubject
import io.github.bertie_mc.gradle.model.fileExtension
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
            val datapacks = manifest.map { it.datapacks }
            val resourcepacks = manifest.map { it.resourcepacks }
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
            val packDatapacks =
                configurations.dependencyScope("packDatapacks") {
                    description = "Datapacks included in the generated pack"
                    useDirectArtifactsOnly()
                }
            val packResourcepacks =
                configurations.dependencyScope("packResourcepacks") {
                    description = "Resourcepacks included in the generated pack"
                    useDirectArtifactsOnly()
                }
            val gameTestDatapacks =
                configurations.dependencyScope("gametestDatapacks") {
                    description = "Server-side datapacks included in the game test instance"
                    useDirectArtifactsOnly()
                }
            val clientTestDatapacks =
                configurations.dependencyScope("clienttestDatapacks") {
                    description = "Client-side datapacks included in the client test instance"
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
                dependencies.add(packMods.name, artifact.notation(artifact.gradleSource))
            }
            shaderpacks.get().forEach { artifact ->
                dependencies.add(
                    packShaderpacks.name,
                    artifact.notation(artifact.gradleSource),
                )
            }
            datapacks.get().forEach { artifact ->
                dependencies.add(
                    packDatapacks.name,
                    artifact.notation(artifact.gradleSource),
                )
                if (artifact.side.isIncludedOn(MinecraftArtifactSide.SERVER)) {
                    dependencies.add(
                        gameTestDatapacks.name,
                        artifact.notation(artifact.gradleSource),
                    )
                }
                if (artifact.side.isIncludedOn(MinecraftArtifactSide.CLIENT)) {
                    dependencies.add(
                        clientTestDatapacks.name,
                        artifact.notation(artifact.gradleSource),
                    )
                }
            }
            resourcepacks.get().forEach { artifact ->
                dependencies.add(
                    packResourcepacks.name,
                    artifact.notation(artifact.gradleSource),
                )
            }

            val modArtifacts = artifactClasspath("packModArtifacts", packMods)
            val gameTestDatapackArtifacts =
                artifactClasspath("gametestDatapackArtifacts", gameTestDatapacks)
            val clientTestDatapackArtifacts =
                artifactClasspath("clienttestDatapackArtifacts", clientTestDatapacks)
            val resourcepackArtifacts = artifactClasspath("packResourcepackArtifacts", packResourcepacks)
            val shaderpackArtifacts = artifactClasspath("packShaderpackArtifacts", packShaderpacks)
            val datapackFilenames = datapacks.map(::filenamesByResolvedFile)
            val resourcepackFilenames = resourcepacks.map(::filenamesByResolvedFile)
            val shaderpackFilenames = shaderpacks.map(::filenamesByResolvedFile)
            val modPackagingArtifacts = packagingClasspath("packModPackagingArtifacts", externalMods)
            val datapackPackagingArtifacts =
                packagingClasspath(
                    "packDatapackPackagingArtifacts",
                    datapacks,
                )
            val resourcepackPackagingArtifacts =
                packagingClasspath(
                    "packResourcepackPackagingArtifacts",
                    resourcepacks,
                )
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
                layout.projectDirectory.dir("datapacks").takeIf { it.asFile.isDirectory }?.let {
                    datapackDirectory.set(it)
                }
                layout.projectDirectory.dir("resourcepacks").takeIf { it.asFile.isDirectory }?.let {
                    resourcepackDirectory.set(it)
                }
                minecraftVersion.set(platform.minecraft)
                neoForgeVersion.set(platform.neoForge)
                outputDirectory.set(layout.buildDirectory.dir("packwiz"))
                packArtifacts.addAll(
                    modPackagingArtifacts.get().externalPackwizArtifacts(externalMods),
                )
                packArtifacts.addAll(
                    datapackPackagingArtifacts.get().externalPackwizArtifacts(datapacks),
                )
                packArtifacts.addAll(
                    resourcepackPackagingArtifacts.get().externalPackwizArtifacts(resourcepacks),
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
                    from(layout.projectDirectory.dir("datapacks")) { into("datapacks") }
                    from(gameTestDatapackArtifacts) {
                        into("datapacks")
                        eachFile { name = datapackFilenames.get()[name] ?: name }
                    }
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
                    from(layout.projectDirectory.dir("datapacks")) { into("datapacks") }
                    from(layout.projectDirectory.dir("resourcepacks")) { into("resourcepacks") }
                    from(clientTestDatapackArtifacts) {
                        into("datapacks")
                        eachFile { name = datapackFilenames.get()[name] ?: name }
                    }
                    from(resourcepackArtifacts) {
                        into("resourcepacks")
                        eachFile { name = resourcepackFilenames.get()[name] ?: name }
                    }
                    from(shaderpackArtifacts) {
                        into("shaderpacks")
                        eachFile { name = shaderpackFilenames.get()[name] ?: name }
                    }
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

private fun filenamesByResolvedFile(artifacts: List<MinecraftArtifact>): Map<String, String> =
    artifacts.associate { artifact ->
        resolvedFileName(artifact) to artifact.filename(artifact.gradleSource)
    }

private fun resolvedFileName(artifact: MinecraftArtifact): String {
    val source = artifact.gradleSource
    val extension = source.fileExtension ?: artifact.kind.extension
    return "${source.module}-${source.version}.$extension"
}
