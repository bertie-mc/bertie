import io.github.bertie_mc.gradle.BertiePlatformVersions
import io.github.bertie_mc.gradle.BertieTestingExtension
import io.github.bertie_mc.gradle.MINECRAFT_1_21_1_LWJGL_MODULES
import io.github.bertie_mc.gradle.bertiePlatformVersions
import io.github.bertie_mc.gradle.lwjglNativeClassifier
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    `java-library`
    idea
    id("net.neoforged.moddev")
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val platformVersions = versionCatalog.bertiePlatformVersions()
extensions.add(BertiePlatformVersions::class.java, "bertiePlatform", platformVersions)

fun catalogVersion(name: String): String =
    versionCatalog.findVersion(name).orElseThrow {
        IllegalStateException("Version '$name' is missing from the root version catalog")
    }.requiredVersion

repositories {
    val central = mavenCentral()
    exclusiveContent {
        forRepositories(central)
        filter {
            includeGroup("org.lwjgl")
            includeGroup("io.github.imurx")
            includeGroup("com.github.ramanrajarathinam")
        }
    }
    maven {
        name = "NeoForge"
        url = uri("https://maven.neoforged.net/releases")
        content { includeGroupByRegex("net\\.neoforged(\\..*)?") }
    }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content { includeGroup("maven.modrinth") }
        metadataSources { artifact() }
    }
    maven {
        name = "CurseMaven"
        url = uri("https://www.cursemaven.com")
        content { includeGroup("curse.maven") }
    }
    maven {
        name = "Create"
        url = uri("https://maven.createmod.net")
        content {
            includeGroup("com.simibubi.create")
            includeGroup("net.createmod.ponder")
            includeGroup("dev.engine-room.flywheel")
        }
    }
    maven {
        name = "Architectury"
        url = uri("https://maven.architectury.dev")
        content { includeGroup("dev.architectury") }
    }
    maven {
        name = "FTB"
        url = uri("https://maven.ftb.dev/releases")
        content { includeGroup("dev.ftb.mods") }
    }
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

dependencyLocking {
    lockAllConfigurations()
}

tasks.register("resolveAndLockAll") {
    group = "build setup"
    description = "Resolves every dependency configuration and writes its lock state"
    notCompatibleWithConfigurationCache("Resolves configurations at execution time")

    doFirst {
        require(gradle.startParameter.isWriteDependencyLocks) {
            "resolveAndLockAll must be run with --write-locks"
        }
    }
    doLast {
        configurations.filter { it.isCanBeResolved }.forEach { it.resolve() }
    }
}

// Minecraft 1.21.1's library manifest only names the generic Linux LWJGL natives,
// which are x86-64 artifacts. Keep the Java module graph identical across
// architectures and add the alternative native classifier on ARM64.
val selectedLwjglNativeClassifier = lwjglNativeClassifier(
    providers.systemProperty("os.name").get(),
    providers.systemProperty("os.arch").get(),
)
val lwjglVersion = catalogVersion("lwjgl")

val testing = extensions.create<BertieTestingExtension>("bertieTesting")
testing.subjectId.convention(project.name.lowercase().replace(Regex("[^a-z0-9_]"), ""))
testing.gameTestNamespace.convention(testing.subjectId)
testing.license.convention("All Rights Reserved")
testing.mainInstanceDirectory.convention(layout.projectDirectory.dir("src/main/instance"))

extensions.configure<NeoForgeExtension> {
    version = platformVersions.neoForge

    parchment {
        minecraftVersion = catalogVersion("parchment-minecraft")
        mappingsVersion = catalogVersion("parchment-mappings")
    }

    runs.configureEach {
        MINECRAFT_1_21_1_LWJGL_MODULES.forEach { module ->
            additionalRuntimeClasspathConfiguration.dependencies.add(
                project.dependencies.create("org.lwjgl:$module:$lwjglVersion"),
            )
            if (selectedLwjglNativeClassifier != null) {
                additionalRuntimeClasspathConfiguration.dependencies.add(
                    project.dependencies.create(
                        "org.lwjgl:$module:$lwjglVersion:$selectedLwjglNativeClassifier",
                    ),
                )
            }
        }
        loadedMods.set(mods.filter { mod ->
            when (name) {
                "gameTests" -> !mod.name.endsWith("_clienttests")
                "clientTests" -> !mod.name.endsWith("_gametests")
                else -> !mod.name.endsWith("_gametests") && !mod.name.endsWith("_clienttests")
            }
        })
    }
}
