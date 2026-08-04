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

val resolvableConfigurations = configurations.matching {
    it.isCanBeResolved && it.name != "kotlinNativeBundleConfiguration"
}

tasks.register("resolveDependencies") {
    group = "build setup"
    description = "Resolves dependencies used by the included build"
    notCompatibleWithConfigurationCache("Resolves configurations at execution time")

    doLast {
        resolvableConfigurations.forEach { it.resolve() }
    }
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
        resolvableConfigurations.forEach { it.resolve() }
    }
}

tasks.test {
    useJUnitPlatform()
}
