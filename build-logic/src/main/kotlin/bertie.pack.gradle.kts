import io.github.bertie_mc.gradle.BertiePlatformVersions
import io.github.bertie_mc.gradle.BertieTestingExtension
import io.github.bertie_mc.gradle.externalPackwizArtifacts
import io.github.bertie_mc.gradle.ownedPackFiles
import io.github.bertie_mc.gradle.packagingClasspath
import io.github.bertie_mc.gradle.parseMinecraftArtifacts
import io.github.bertie_mc.gradle.useDirectArtifactsOnly
import io.github.bertie_mc.gradle.tasks.GeneratePackwizPack
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.slf4j.event.Level

plugins {
    id("bertie.neoforge-base")
}

val manifest = providers.fileContents(
    rootProject.layout.projectDirectory.file("gradle/minecraft-artifacts.toml"),
).asText.map(::parseMinecraftArtifacts)
val externalMods = manifest.map { artifacts -> artifacts.mods }
val shaderpacks = manifest.map { artifacts -> artifacts.shaderpacks }

val packRuntime = configurations.dependencyScope("packRuntime") {
    description = "Complete declared third-party and owned mod inventory"
}
val packShaderpacks = configurations.dependencyScope("packShaderpacks") {
    description = "Shaderpacks included in the generated pack"
}

fun artifactClasspath(
    name: String,
    bucket: Provider<out Configuration>,
): Provider<out Configuration> = configurations.resolvable(name) {
    description = "Direct artifacts from ${bucket.get().name}"
    extendsFrom(bucket.get())
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

listOf(packRuntime, packShaderpacks).forEach { bucket ->
    bucket.configure { useDirectArtifactsOnly() }
}

externalMods.get().forEach { artifact ->
    dependencies.add(packRuntime.name, artifact.gradleSource.notation())
}
shaderpacks.get().forEach { artifact ->
    dependencies.add(packShaderpacks.name, artifact.gradleSource.notation(artifact.kind.extension))
}

val packRuntimeArtifacts = artifactClasspath("packRuntimeArtifacts", packRuntime)
val packShaderpackArtifacts = artifactClasspath("packShaderpackArtifacts", packShaderpacks)
val packRuntimePackagingArtifacts =
    project.packagingClasspath("packRuntimePackagingArtifacts", externalMods)
val packShaderpackPackagingArtifacts =
    project.packagingClasspath("packShaderpackPackagingArtifacts", shaderpacks)

configurations.named("implementation") {
    extendsFrom(packRuntime.get())
}

val testing = extensions.getByType<BertieTestingExtension>().apply {
    subjectId.set("bertiepacktests")
    gameTestNamespace.set("bertiepacktests")
    license.set("Unlicense")
    mainInstanceDirectory.set(layout.buildDirectory.dir("generated/main-instance"))
}

val platform = extensions.getByType<BertiePlatformVersions>()
tasks.register<GeneratePackwizPack>("generatePackwiz") {
    group = "distribution"
    description = "Generates the complete packwiz pack from Gradle project and artifact inputs"
    packProperties.set(layout.projectDirectory.file("pack.properties"))
    contentDirectory.set(layout.projectDirectory.dir("config"))
    minecraftVersion.set(platform.minecraft)
    neoForgeVersion.set(platform.neoForge)
    outputDirectory.set(layout.buildDirectory.dir("packwiz"))
    packArtifacts.addAll(
        packRuntimePackagingArtifacts.get().externalPackwizArtifacts(externalMods),
    )
    packArtifacts.addAll(
        packShaderpackPackagingArtifacts.get().externalPackwizArtifacts(shaderpacks),
    )
    localModFiles.from(packRuntimeArtifacts.get().ownedPackFiles())
    dependsOn(
        packRuntimeArtifacts,
        packRuntimePackagingArtifacts,
        packShaderpackPackagingArtifacts,
    )
}

val preparePackMainInstance = tasks.register<Sync>("preparePackMainInstance") {
    group = "verification"
    description = "Stages pack-owned instance files without consuming packwiz output"
    into(testing.mainInstanceDirectory)
    from(layout.projectDirectory.dir("config")) {
        into("config")
    }
}

pluginManager.withPlugin("bertie.gametest") {
    tasks.named<Sync>("prepareGametestInstance") {
        dependsOn(preparePackMainInstance)
    }
    extensions.configure<NeoForgeExtension> {
        runs.named("gameTests") {
            jvmArgument("-Xmx8G")
            logLevel.set(Level.INFO)
        }
    }
}

pluginManager.withPlugin("bertie.client-test") {
    tasks.named<Sync>("prepareClienttestInstance") {
        dependsOn(preparePackMainInstance)
        from(packShaderpackArtifacts) {
            into("shaderpacks")
        }
    }
    extensions.configure<NeoForgeExtension> {
        runs.named("clientTests") {
            jvmArgument("-Xmx10G")
            logLevel.set(Level.INFO)
        }
    }
}
