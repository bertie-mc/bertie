package io.github.bertie_mc.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin

class BertieRootPlugin : Plugin<Project> {
    override fun apply(project: Project) =
        with(project) {
            pluginManager.apply(BasePlugin::class.java)
            pluginManager.apply(FormattingPlugin::class.java)

            tasks.register("prepareOfflineBuild") {
                group = "build setup"
                description = "Prepares repository dependencies for offline build and test jobs"
                dependsOn(
                    gradle.includedBuild("build-logic").task(":resolveDependencies"),
                    subprojects
                        .filter { it.childProjects.isEmpty() }
                        .map { "${it.path}:prepareOfflineBuild" },
                    "spotlessJava",
                    "spotlessKotlin",
                    "spotlessKotlinGradle",
                )
            }

            tasks.register("resolveAndLockAll") {
                group = "build setup"
                description = "Resolves repository dependencies and writes their lock state"
                dependsOn(
                    gradle.includedBuild("build-logic").task(":resolveAndLockAll"),
                    subprojects
                        .filter { it.childProjects.isEmpty() }
                        .map { "${it.path}:resolveAndLockAll" },
                )
            }

            val testInfrastructure =
                tasks.register("testInfrastructure") {
                    group = "verification"
                    description = "Runs tests for shared Gradle and in-game testing infrastructure"
                    dependsOn(
                        gradle.includedBuild("build-logic").task(":test"),
                        ":core:client-test-api:test",
                        ":core:client-test-driver:test",
                        ":core:gametest-driver:test",
                    )
                }

            tasks.named("check").configure {
                dependsOn("spotlessCheck", testInfrastructure)
            }
        }
}
