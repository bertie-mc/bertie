package io.github.bertie_mc.gradle.plugins

import io.github.bertie_mc.gradle.conventions.configureJvmRole
import io.github.bertie_mc.gradle.conventions.configureNeoForgeRole
import io.github.bertie_mc.gradle.conventions.externalPackwizArtifacts
import io.github.bertie_mc.gradle.conventions.ownedPackFiles
import io.github.bertie_mc.gradle.conventions.packagingClasspath
import io.github.bertie_mc.gradle.conventions.useLockedArtifactTypes
import io.github.bertie_mc.gradle.model.CurseForgeArtifactSource
import io.github.bertie_mc.gradle.model.CurseForgeManifestArtifact
import io.github.bertie_mc.gradle.model.MinecraftArtifact
import io.github.bertie_mc.gradle.model.MinecraftArtifactKind
import io.github.bertie_mc.gradle.model.MinecraftArtifactManifest
import io.github.bertie_mc.gradle.model.MinecraftArtifactSide
import io.github.bertie_mc.gradle.model.PlatformVersions
import io.github.bertie_mc.gradle.model.TestSubject
import io.github.bertie_mc.gradle.model.fileExtension
import io.github.bertie_mc.gradle.model.loadMinecraftArtifacts
import io.github.bertie_mc.gradle.model.loadRedistributionPolicy
import io.github.bertie_mc.gradle.tasks.GenerateCurseForgePack
import io.github.bertie_mc.gradle.tasks.GenerateMrpack
import io.github.bertie_mc.gradle.tasks.GeneratePackwizPack
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
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

            val development =
                loadMinecraftArtifacts(layout.settingsDirectory.asFile, "development")
            val modrinthRelease =
                loadMinecraftArtifacts(layout.settingsDirectory.asFile, "release-modrinth")
            val curseForgeRelease =
                loadMinecraftArtifacts(layout.settingsDirectory.asFile, "release-curseforge")

            val packComponents =
                configurations.dependencyScope("packComponents") {
                    description = "Explicit third-party logical roots in the complete pack"
                }
            val gameTestComponents =
                configurations.dependencyScope("gametestComponents") {
                    description = "Third-party logical roots installed for full-pack GameTests"
                }
            val clientTestComponents =
                configurations.dependencyScope("clienttestComponents") {
                    description = "Third-party logical roots installed for full-pack client tests"
                }
            val packMods =
                configurations.dependencyScope("packMods") {
                    description = "Bertie-owned mods included in the complete pack"
                }
            val dependencyHandler = dependencies

            val packRoots = componentRoots(packComponents, development)
            val gameTestRoots = componentRoots(gameTestComponents, development)
            val clientTestRoots = componentRoots(clientTestComponents, development)
            val gameTestArtifacts = gameTestRoots.map(development::reachableArtifacts)
            val clientTestArtifacts = clientTestRoots.map(development::reachableArtifacts)

            val gameTestComponentClasspath =
                componentClasspath("gametestComponentArtifacts", gameTestComponents)
            val clientTestComponentClasspath =
                componentClasspath("clienttestComponentArtifacts", clientTestComponents)
            val ownedModArtifacts = componentClasspath("ownedPackModArtifacts", packMods)
            val ownedRuntimeComponents = externalComponents(ownedModArtifacts, development)
            val modrinthReleaseArtifacts =
                packRoots.zip(ownedRuntimeComponents) { neutralRoots, modRoots ->
                    (
                        modrinthRelease.reachableArtifacts(neutralRoots) +
                            modrinthRelease.reachableArtifacts(modRoots, requireMod = true)
                    ).distinctBy(MinecraftArtifact::identity)
                }
            val curseForgeReleaseArtifacts =
                packRoots.zip(ownedRuntimeComponents) { neutralRoots, modRoots ->
                    (
                        curseForgeRelease.reachableArtifacts(neutralRoots) +
                            curseForgeRelease.reachableArtifacts(modRoots, requireMod = true)
                    ).distinctBy(MinecraftArtifact::identity)
                }

            val gameTestMods =
                lockedFiles(
                    gameTestComponentClasspath,
                    development,
                    MinecraftArtifactSide.SERVER,
                    MinecraftArtifactKind.MOD,
                )
            val clientTestMods =
                lockedFiles(
                    clientTestComponentClasspath,
                    development,
                    MinecraftArtifactSide.CLIENT,
                    MinecraftArtifactKind.MOD,
                )
            val gameTestDatapacks =
                lockedFiles(
                    gameTestComponentClasspath,
                    development,
                    MinecraftArtifactSide.SERVER,
                    MinecraftArtifactKind.DATAPACK,
                )
            val clientTestDatapacks =
                lockedFiles(
                    clientTestComponentClasspath,
                    development,
                    MinecraftArtifactSide.CLIENT,
                    MinecraftArtifactKind.DATAPACK,
                )
            val clientTestResourcepacks =
                lockedFiles(
                    clientTestComponentClasspath,
                    development,
                    MinecraftArtifactSide.CLIENT,
                    MinecraftArtifactKind.RESOURCEPACK,
                )
            val clientTestShaderpacks =
                lockedFiles(
                    clientTestComponentClasspath,
                    development,
                    MinecraftArtifactSide.CLIENT,
                    MinecraftArtifactKind.SHADERPACK,
                )

            val modrinthReleaseMods =
                modrinthReleaseArtifacts.map { artifacts -> artifacts.ofKind(MinecraftArtifactKind.MOD) }
            val modrinthReleaseDatapacks =
                modrinthReleaseArtifacts.map { artifacts -> artifacts.ofKind(MinecraftArtifactKind.DATAPACK) }
            val modrinthReleaseResourcepacks =
                modrinthReleaseArtifacts.map { artifacts -> artifacts.ofKind(MinecraftArtifactKind.RESOURCEPACK) }
            val modrinthReleaseShaderpacks =
                modrinthReleaseArtifacts.map { artifacts -> artifacts.ofKind(MinecraftArtifactKind.SHADERPACK) }
            val modPackagingArtifacts = packagingClasspath("packModPackagingArtifacts", modrinthReleaseMods)
            val datapackPackagingArtifacts =
                packagingClasspath("packDatapackPackagingArtifacts", modrinthReleaseDatapacks)
            val resourcepackPackagingArtifacts =
                packagingClasspath("packResourcepackPackagingArtifacts", modrinthReleaseResourcepacks)
            val shaderpackPackagingArtifacts =
                packagingClasspath("packShaderpackPackagingArtifacts", modrinthReleaseShaderpacks)
            val modrinthReleaseModFiles =
                modPackagingArtifacts.get().externalPackwizArtifacts(modrinthReleaseMods)
            val modrinthReleaseDatapackFiles =
                datapackPackagingArtifacts.get().externalPackwizArtifacts(modrinthReleaseDatapacks)
            val modrinthReleaseResourcepackFiles =
                resourcepackPackagingArtifacts.get().externalPackwizArtifacts(modrinthReleaseResourcepacks)
            val modrinthReleaseShaderpackFiles =
                shaderpackPackagingArtifacts.get().externalPackwizArtifacts(modrinthReleaseShaderpacks)
            val modrinthReleaseFiles =
                modrinthReleaseModFiles
                    .zip(modrinthReleaseDatapackFiles) { first, second -> first + second }
                    .zip(modrinthReleaseResourcepackFiles) { first, second -> first + second }
                    .zip(modrinthReleaseShaderpackFiles) { first, second -> first + second }

            val curseForgeClientArtifacts =
                curseForgeReleaseArtifacts.map { artifacts ->
                    artifacts.filter { artifact ->
                        artifact.side.isIncludedOn(MinecraftArtifactSide.CLIENT)
                    }
                }
            val curseForgeNativeArtifacts =
                curseForgeClientArtifacts.map { artifacts ->
                    artifacts.mapNotNull { artifact ->
                        val source = artifact.source as? CurseForgeArtifactSource ?: return@mapNotNull null
                        CurseForgeManifestArtifact(
                            projectId = source.projectId,
                            fileId = source.fileId,
                        )
                    }
                }
            val curseForgeEmbeddedArtifacts =
                curseForgeClientArtifacts.map { artifacts ->
                    artifacts.filter { artifact -> artifact.source !is CurseForgeArtifactSource }
                }
            val curseForgeEmbeddedClasspath =
                packagingClasspath(
                    "packCurseForgeEmbeddedArtifacts",
                    curseForgeEmbeddedArtifacts,
                )
            val curseForgeEmbeddedFiles =
                curseForgeEmbeddedClasspath
                    .get()
                    .externalPackwizArtifacts(curseForgeEmbeddedArtifacts)

            val gameTestFilenames = gameTestArtifacts.map(::filenamesByResolvedFile)
            val clientTestFilenames = clientTestArtifacts.map(::filenamesByResolvedFile)

            val platform = extensions.getByType<PlatformVersions>()
            tasks.register<GeneratePackwizPack>("generatePackwiz") {
                group = "distribution"
                description = "Generates the Modrinth release projection as packwiz conversion output"
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
                packArtifacts.addAll(modrinthReleaseFiles)
                localModFiles.from(ownedModArtifacts.ownedPackFiles())
            }
            val redistribution = loadRedistributionPolicy(layout.settingsDirectory.asFile)
            tasks.register<GenerateMrpack>("generateMrpack") {
                group = "distribution"
                description = "Generates the Modrinth client pack directly from the release lock"
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
                externalArtifacts.addAll(modrinthReleaseFiles)
                localModFiles.from(ownedModArtifacts.ownedPackFiles())
                redistributionStrict.set(redistribution.strict)
                redistributionEvidence.set(redistribution.evidence)
                redistributionArtifacts.set(redistribution.artifactsFor("modrinth"))
                outputFile.set(layout.buildDirectory.file("distributions/bertie.mrpack"))
                embeddingAuditFile.set(
                    layout.buildDirectory.file("reports/dependencies/release-modrinth-embedding.txt"),
                )
            }
            tasks.register<GenerateCurseForgePack>("generateCurseForgePack") {
                group = "distribution"
                description = "Generates the CurseForge client pack directly from the release lock"
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
                nativeArtifacts.addAll(curseForgeNativeArtifacts)
                embeddedArtifacts.addAll(curseForgeEmbeddedFiles)
                localModFiles.from(ownedModArtifacts.ownedPackFiles())
                redistributionStrict.set(redistribution.strict)
                redistributionEvidence.set(redistribution.evidence)
                redistributionArtifacts.set(redistribution.artifactsFor("curseforge"))
                outputFile.set(layout.buildDirectory.file("distributions/bertie-curseforge.zip"))
                embeddingAuditFile.set(
                    layout.buildDirectory.file("reports/dependencies/release-curseforge-embedding.txt"),
                )
            }

            pluginManager.withPlugin("bertie.gametest") {
                configurations.named("gametestRuntimeOnly") {
                    extendsFrom(packMods.get())
                    dependencies.add(dependencyHandler.create(files(gameTestMods)))
                }
                tasks.named<Sync>("prepareGameTestInstance") {
                    from(layout.projectDirectory.dir("config")) { into("config") }
                    from(layout.projectDirectory.dir("datapacks")) { into("datapacks") }
                    from(gameTestDatapacks) {
                        into("datapacks")
                        eachFile { name = gameTestFilenames.get()[name] ?: name }
                    }
                }
                tasks.named("runGameTests") { timeout.set(Duration.ofMinutes(75)) }
                neoForge.runs.named("gameTests") { jvmArgument("-Xmx8G") }
            }
            pluginManager.withPlugin("bertie.client-test") {
                configurations.named("clienttestRuntimeOnly") {
                    extendsFrom(packMods.get())
                    dependencies.add(dependencyHandler.create(files(clientTestMods)))
                }
                tasks.named<Sync>("prepareClientTestInstance") {
                    from(layout.projectDirectory.dir("config")) { into("config") }
                    from(layout.projectDirectory.dir("datapacks")) { into("datapacks") }
                    from(layout.projectDirectory.dir("resourcepacks")) { into("resourcepacks") }
                    from(clientTestDatapacks) {
                        into("datapacks")
                        eachFile { name = clientTestFilenames.get()[name] ?: name }
                    }
                    from(clientTestResourcepacks) {
                        into(MinecraftArtifactKind.RESOURCEPACK.destination)
                        eachFile { name = clientTestFilenames.get()[name] ?: name }
                    }
                    from(clientTestShaderpacks) {
                        into("shaderpacks")
                        eachFile { name = clientTestFilenames.get()[name] ?: name }
                    }
                }
                tasks.named("runClientTests") { timeout.set(Duration.ofMinutes(105)) }
                neoForge.runs.named("clientTests") { jvmArgument("-Xmx10G") }
            }
        }

    private fun Project.componentRoots(
        bucket: Provider<out Configuration>,
        manifest: MinecraftArtifactManifest,
    ): Provider<Set<String>> {
        bucket.get().useLockedArtifactTypes(manifest)
        return providers.provider {
            val byCoordinate =
                manifest.components.keys.associateBy { component ->
                    manifest.selectedArtifact(component).source.coordinate()
                }
            bucket.get().allDependencies.mapNotNullTo(sortedSetOf()) { dependency ->
                val external = dependency as? ExternalModuleDependency ?: return@mapNotNullTo null
                val coordinate =
                    "${external.group}:${external.name}:${external.versionConstraint.requiredVersion}"
                byCoordinate[coordinate]
                    ?: error(
                        "${bucket.get().name} dependency '$coordinate' is not a locked " +
                            "Minecraft component; use the deps catalog",
                    )
            }
        }
    }

    private fun Project.componentClasspath(
        name: String,
        bucket: Provider<out Configuration>,
    ): Configuration =
        configurations
            .resolvable(name) {
                description = "Locked component artifacts from ${bucket.get().name}"
                extendsFrom(bucket.get())
                attributes {
                    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
                    attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.EXTERNAL))
                    attribute(
                        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        objects.named(LibraryElements::class.java, LibraryElements.JAR),
                    )
                }
            }.get()

    private fun Project.externalComponents(
        configuration: Configuration,
        manifest: MinecraftArtifactManifest,
    ): Provider<Set<String>> =
        providers.provider {
            val byCoordinate =
                manifest.selections
                    .mapNotNull { (component, selection) ->
                        selection.mod?.let { identity ->
                            manifest.artifacts
                                .getValue(identity)
                                .source
                                .coordinate() to component
                        }
                    }.toMap()
            configuration.incoming.resolutionResult.allComponents.mapNotNullTo(sortedSetOf()) { result ->
                val identifier = result.id as? ModuleComponentIdentifier ?: return@mapNotNullTo null
                byCoordinate[identifier.coordinate()]
            }
        }
}

private fun lockedFiles(
    configuration: Configuration,
    manifest: MinecraftArtifactManifest,
    side: MinecraftArtifactSide,
    kind: MinecraftArtifactKind,
): FileCollection {
    val selected =
        manifest.artifacts.values
            .filter { artifact -> kind in artifact.installationKinds && artifact.side.isIncludedOn(side) }
            .mapTo(hashSetOf()) { artifact -> artifact.source.coordinate() }
    return configuration.incoming
        .artifactView {
            componentFilter { identifier ->
                identifier is ModuleComponentIdentifier && identifier.coordinate() in selected
            }
        }.files
}

private fun List<MinecraftArtifact>.ofKind(kind: MinecraftArtifactKind): List<MinecraftArtifact> =
    filter { artifact -> artifact.kind == kind }

private fun filenamesByResolvedFile(artifacts: List<MinecraftArtifact>): Map<String, String> =
    artifacts.associate { artifact -> resolvedFileName(artifact) to artifact.filename }

private fun resolvedFileName(artifact: MinecraftArtifact): String {
    val source = artifact.source
    val extension = source.fileExtension ?: artifact.kind.extension
    return "${source.module}-${source.version}.$extension"
}

private fun io.github.bertie_mc.gradle.model.MinecraftArtifactSource.coordinate(): String = "$group:$module:$version"

private fun ModuleComponentIdentifier.coordinate(): String = "$group:$module:$version"
