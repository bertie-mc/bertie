import org.gradle.api.artifacts.component.ModuleComponentIdentifier

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.moddev.gradle)
    implementation(libs.night.config.toml)
    implementation(libs.gson)
    implementation(libs.shadow.gradle.plugin)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

gradlePlugin {
    plugins {
        create("settings") {
            id = "bertie.settings"
            implementationClass =
                "io.github.bertie_mc.gradle.settings.BertieSettingsPlugin"
        }
        create("minecraftInputs") {
            id = "bertie.minecraft-inputs"
            implementationClass =
                "io.github.bertie_mc.gradle.plugins.MinecraftInputsPlugin"
        }
        create("root") {
            id = "bertie.root"
            implementationClass =
                "io.github.bertie_mc.gradle.plugins.BertieRootPlugin"
        }
        create("minecraftLibrary") {
            id = "bertie.minecraft-library"
            implementationClass =
                "io.github.bertie_mc.gradle.plugins.MinecraftLibraryPlugin"
        }
        create("mod") {
            id = "bertie.mod"
            implementationClass =
                "io.github.bertie_mc.gradle.plugins.ModPlugin"
        }
        create("gameTest") {
            id = "bertie.gametest"
            implementationClass =
                "io.github.bertie_mc.gradle.plugins.GameTestPlugin"
        }
        create("clientTest") {
            id = "bertie.client-test"
            implementationClass =
                "io.github.bertie_mc.gradle.plugins.ClientTestPlugin"
        }
        create("pack") {
            id = "bertie.pack"
            implementationClass =
                "io.github.bertie_mc.gradle.plugins.PackPlugin"
        }
        create("developmentRuns") {
            id = "bertie.dev-runs"
            implementationClass =
                "io.github.bertie_mc.gradle.plugins.DevelopmentRunsPlugin"
        }
        create("dataGeneration") {
            id = "bertie.datagen"
            implementationClass =
                "io.github.bertie_mc.gradle.plugins.DataGenerationPlugin"
        }
        create("neoForgeTest") {
            id = "bertie.neoforge-test"
            implementationClass =
                "io.github.bertie_mc.gradle.plugins.NeoForgeTestPlugin"
        }
        create("jarJar") {
            id = "bertie.jarjar"
            implementationClass =
                "io.github.bertie_mc.gradle.plugins.JarJarPlugin"
        }
        create("shadedMod") {
            id = "bertie.shaded-mod"
            implementationClass =
                "io.github.bertie_mc.gradle.plugins.ShadedModPlugin"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencyLocking {
    lockAllConfigurations()
}

val resolveDependencies = tasks.register("resolveDependencies") {
    group = "build setup"
    description = "Resolves dependencies used by the included build"
}

val resolveAndLockAll = tasks.register("resolveAndLockAll") {
    group = "build setup"
    description = "Resolves every relevant dependency configuration and writes its lock state"
}

listOf(
    "compileClasspath",
    "runtimeClasspath",
    "testCompileClasspath",
    "testRuntimeClasspath",
    "compilePluginsBlocksPluginClasspathElements",
    "kotlinBuildToolsApiClasspath",
    "kotlinCompilerClasspath",
    "kotlinCompilerPluginClasspathMain",
    "kotlinCompilerPluginClasspathTest",
    "kotlinKlibCommonizerClasspath",
).forEach { configurationName ->
    configurations.named(configurationName) {
        val resolvedArtifacts = incoming.artifacts.resolvedArtifacts
        val externalArtifacts = providers.provider {
            resolvedArtifacts.get().asSequence()
                .filter { it.id.componentIdentifier is ModuleComponentIdentifier }
                .map { it.file }
                .toList()
        }
        resolveDependencies.configure { inputs.files(externalArtifacts) }
        resolveAndLockAll.configure { inputs.files(externalArtifacts) }
    }
}

tasks.test {
    useJUnitPlatform()
}
