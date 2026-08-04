plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.moddev.gradle)
    implementation(libs.night.config.toml)
    implementation(libs.shadow.gradle.plugin)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

gradlePlugin {
    plugins {
        create("minecraftArtifacts") {
            id = "bertie.minecraft-artifacts"
            implementationClass =
                "io.github.bertie_mc.gradle.MinecraftArtifactsSettingsPlugin"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencyLocking {
    lockAllConfigurations()
}

tasks.register("resolveAndLockAll") {
    group = "build setup"
    description = "Resolves every relevant dependency configuration and writes its lock state"
    notCompatibleWithConfigurationCache("Resolves configurations at execution time")

    doFirst {
        require(gradle.startParameter.isWriteDependencyLocks) {
            "resolveAndLockAll must be run with --write-locks"
        }
    }
    doLast {
        configurations
            // The Kotlin JVM plugin creates this unused native-toolchain configuration.
            .filter { it.isCanBeResolved && it.name != "kotlinNativeBundleConfiguration" }
            .forEach { it.resolve() }
    }
}

tasks.test {
    useJUnitPlatform()
}
