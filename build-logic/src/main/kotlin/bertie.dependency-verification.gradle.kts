import io.github.bertie_mc.gradle.MINECRAFT_1_21_1_LWJGL_MODULES
import io.github.bertie_mc.gradle.MINECRAFT_1_21_1_LWJGL_NATIVE_CLASSIFIERS
import org.gradle.api.artifacts.VersionCatalogsExtension

repositories {
    mavenCentral {
        content { includeGroup("org.lwjgl") }
    }
}

// Keep cross-platform Minecraft natives in shared verification metadata even when
// that metadata is regenerated on a single host. This graph is resolution-only.
val dependencyVerificationSeed by configurations.creating {
    description = "Cross-platform Minecraft artifacts included in dependency verification metadata"
    isCanBeConsumed = false
    isCanBeResolved = true
    isVisible = false
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val lwjglVersion = versionCatalog.findVersion("lwjgl").orElseThrow {
    IllegalStateException("Version 'lwjgl' is missing from the root version catalog")
}.requiredVersion

dependencies {
    MINECRAFT_1_21_1_LWJGL_MODULES.forEach { module ->
        MINECRAFT_1_21_1_LWJGL_NATIVE_CLASSIFIERS
            .filterNot { module == "lwjgl-freetype" && it == "natives-macos" }
            .forEach { classifier ->
                add(
                    dependencyVerificationSeed.name,
                    "org.lwjgl:$module:$lwjglVersion:$classifier",
                )
            }
    }
}

tasks.register("resolveDependencyVerificationSeed") {
    group = "build setup"
    description = "Resolves cross-platform artifacts while refreshing verification metadata"
    notCompatibleWithConfigurationCache("Resolves a configuration at execution time")
    doLast {
        dependencyVerificationSeed.resolve()
    }
}
