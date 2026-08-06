package io.github.bertie_mc.gradle.conventions

import io.github.bertie_mc.gradle.model.PlatformVersions
import io.github.bertie_mc.gradle.model.lwjglNativeClassifier
import io.github.bertie_mc.gradle.model.platformVersions
import io.github.bertie_mc.gradle.tasks.ResolveDependencies
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.dsl.RunModel
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

internal fun Project.configureJvmRole() {
    pluginManager.apply(JavaLibraryPlugin::class.java)
    dependencyLocking.lockAllConfigurations()

    extensions.getByType<JavaPluginExtension>().toolchain.languageVersion.set(
        JavaLanguageVersion.of(21),
    )

    val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
    extensions.add(
        PlatformVersions::class.java,
        "bertiePlatform",
        catalog.platformVersions(),
    )
    dependencies.add(
        "testImplementation",
        dependencies.platform(catalog.requiredLibrary("junit-bom")),
    )
    dependencies.add("testImplementation", catalog.requiredLibrary("junit-jupiter"))
    dependencies.add("testRuntimeOnly", catalog.requiredLibrary("junit-launcher"))

    tasks.withType<Test>().configureEach { useJUnitPlatform() }
    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    tasks.register("runTests") {
        group = "verification"
        description = "Runs every test suite enabled for this project"
        dependsOn("test")
    }

    val externalDependencyArtifacts = objects.fileCollection()
    tasks.register<ResolveDependencies>("prepareOfflineBuild") {
        group = "build setup"
        description = "Prepares this project's dependencies for an offline build"
        dependencies.from(externalDependencyArtifacts)
    }
    tasks.register<ResolveDependencies>("resolveAndLockAll") {
        group = "build setup"
        description = "Resolves this project's dependencies and writes lock state"
        dependencies.from(externalDependencyArtifacts)
    }

    configurations.configureEach {
        val configuration = this
        val externalArtifacts =
            providers.provider {
                if (!configuration.isCanBeResolved) {
                    emptyList()
                } else {
                    configuration.incoming.artifacts.resolvedArtifacts
                        .get()
                        .asSequence()
                        .filter { it.id.componentIdentifier is ModuleComponentIdentifier }
                        .map { it.file }
                        .toList()
                }
            }
        externalDependencyArtifacts.from(externalArtifacts)
    }
}

internal fun Project.configureNeoForgeRole(): NeoForgeExtension {
    pluginManager.apply("net.neoforged.moddev")

    val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
    return extensions.getByType<NeoForgeExtension>().apply {
        setVersion(catalog.requiredVersion("neoforge"))
        parchment.parchmentArtifact.set(catalog.requiredLibrary("parchment-data").notation())
        parchment.conflictResolutionPrefix.set("p_")
    }
}

internal fun Project.addArm64LwjglNatives(run: RunModel) {
    addArm64LwjglNatives(run.additionalRuntimeClasspathConfiguration)
}

internal fun Project.addArm64LwjglNatives(configuration: Configuration) {
    val classifier =
        lwjglNativeClassifier(
            providers.systemProperty("os.name").get(),
            providers.systemProperty("os.arch").get(),
        ) ?: return
    val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
    catalog.requiredBundle("lwjgl-natives").forEach { module ->
        configuration.dependencies.add(
            (dependencies.create(module) as ExternalModuleDependency).apply {
                artifact { this.classifier = classifier }
            },
        )
    }
}

internal fun VersionCatalog.requiredVersion(name: String): String =
    findVersion(name)
        .orElseThrow {
            IllegalStateException("Version '$name' is missing from the root version catalog")
        }.requiredVersion

internal fun VersionCatalog.requiredLibrary(name: String): MinimalExternalModuleDependency =
    findLibrary(name)
        .orElseThrow {
            IllegalStateException("Library '$name' is missing from the root version catalog")
        }.get()

internal fun VersionCatalog.requiredBundle(name: String): List<MinimalExternalModuleDependency> =
    findBundle(name)
        .orElseThrow {
            IllegalStateException("Bundle '$name' is missing from the root version catalog")
        }.get()

private fun MinimalExternalModuleDependency.notation(): String = "${module.group}:${module.name}:${versionConstraint.requiredVersion}"
