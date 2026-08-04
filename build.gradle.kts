import org.gradle.api.artifacts.component.ProjectComponentIdentifier

plugins {
    base
    id("bertie.dependency-verification")
}

val warmDependencies = tasks.register("warmDependencies") {
    group = "build setup"
    description = "Resolves all external dependencies and prepares shared Minecraft artifacts"
    notCompatibleWithConfigurationCache("Resolves configurations at execution time")
    dependsOn(
        gradle.includedBuild("build-logic").task(":resolveDependencies"),
        ":pack:createMinecraftArtifacts",
        ":pack:downloadAssets",
    )

    doLast {
        allprojects.forEach { targetProject ->
            targetProject.configurations
                .filter { configuration -> configuration.isCanBeResolved }
                .forEach { configuration ->
                    configuration.incoming.artifactView {
                        componentFilter { identifier -> identifier !is ProjectComponentIdentifier }
                    }.files.files
                }
        }
    }
}

val testInfrastructure = tasks.register("testInfrastructure") {
    group = "verification"
    description = "Runs tests for shared Gradle and in-game testing infrastructure"
    dependsOn(
        gradle.includedBuild("build-logic").task(":test"),
        ":testing:client-test-api:test",
        ":testing:client-test-driver:test",
        ":testing:gametest-driver:test",
    )
}

tasks.named("check") {
    dependsOn(testInfrastructure)
}
