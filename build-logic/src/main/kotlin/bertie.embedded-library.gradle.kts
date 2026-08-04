import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Jar

plugins {
    id("bertie.neoforge-base")
    id("com.gradleup.shadow")
}

val embeddedLibrary = configurations.dependencyScope("embeddedLibrary") {
    description = "Libraries merged into this mod"
}
val embeddedLibraryClasspath = configurations.resolvable("embeddedLibraryClasspath") {
    description = "Resolved libraries merged into this mod"
    extendsFrom(embeddedLibrary.get())
}

configurations.named("compileOnly") {
    extendsFrom(embeddedLibrary.get())
}

fun ShadowJar.includeEmbeddedLibraries() {
    configurations = embeddedLibraryClasspath.map(::listOf)
    exclude(
        "META-INF/INDEX.LIST",
        "META-INF/MANIFEST.MF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
        "META-INF/*.SF",
        "META-INF/versions/*/module-info.class",
        "module-info.class",
    )

    // Service provider files need semantic merging. Any other duplicate resource is
    // ambiguous and should be resolved explicitly instead of depending on classpath order.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
    filesNotMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.FAIL
    }

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val mergedEmbeddedLibraries = tasks.register<ShadowJar>("mergedEmbeddedLibraries") {
    description = "Merges embedded libraries for development runs"
    archiveFileName.set("${project.name}-embedded-libraries.jar")
    destinationDirectory.set(layout.buildDirectory.dir("embedded-libraries"))
    manifest.attributes[
        "Automatic-Module-Name"
    ] = "io.github.bertie_mc.${project.name.replace(Regex("[^A-Za-z0-9]"), "")}.embedded"
    includeEmbeddedLibraries()
}
val mergedEmbeddedLibrariesClasspath = project.files(
    mergedEmbeddedLibraries.flatMap(ShadowJar::getArchiveFile),
).builtBy(mergedEmbeddedLibraries)

extensions.configure<NeoForgeExtension> {
    runs.configureEach {
        taskBefore(mergedEmbeddedLibraries)
        additionalRuntimeClasspathConfiguration.dependencies.add(
            project.dependencies.create(mergedEmbeddedLibrariesClasspath),
        )
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

val embeddedJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    includeEmbeddedLibraries()

    from(layout.projectDirectory.file("NOTICE")) {
        into("META-INF")
    }
    from(layout.projectDirectory.file("UNLICENSE")) {
        into("META-INF")
    }
}

listOf("apiElements", "runtimeElements").forEach { configurationName ->
    configurations.named(configurationName) {
        outgoing.artifacts.clear()
        outgoing.artifact(embeddedJar)
    }
}
configurations.named("shadowRuntimeElements") {
    isCanBeConsumed = false
}

tasks.named("assemble") {
    dependsOn(embeddedJar)
}
