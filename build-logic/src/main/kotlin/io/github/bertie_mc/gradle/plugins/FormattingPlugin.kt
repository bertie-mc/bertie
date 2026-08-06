package io.github.bertie_mc.gradle.plugins

import com.diffplug.gradle.spotless.SpotlessExtension
import io.github.bertie_mc.gradle.conventions.requiredVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named

class FormattingPlugin : Plugin<Project> {
    override fun apply(project: Project) =
        with(project) {
            pluginManager.apply("com.diffplug.spotless")

            val versions = extensions.getByType<VersionCatalogsExtension>().named("libs")
            val sourceDirectories =
                listOf(
                    layout.projectDirectory.dir("src").asFile,
                    layout.projectDirectory.dir("build-logic/src").asFile,
                ) +
                    subprojects.map {
                        it.layout.projectDirectory
                            .dir("src")
                            .asFile
                    }
            val javaSources = sourceDirectories.map { fileTree(it) { include("**/*.java") } }
            val kotlinSources = sourceDirectories.map { fileTree(it) { include("**/*.kt") } }
            val kotlinGradleFiles =
                listOf(
                    buildFile,
                    layout.projectDirectory.file("settings.gradle.kts").asFile,
                    layout.projectDirectory.file("build-logic/build.gradle.kts").asFile,
                    layout.projectDirectory.file("build-logic/settings.gradle.kts").asFile,
                ) + subprojects.map { it.buildFile }

            extensions.configure<SpotlessExtension> {
                java {
                    target(javaSources)
                    palantirJavaFormat(versions.requiredVersion("palantir-java-format"))
                }
                kotlin {
                    target(kotlinSources)
                    ktlint(versions.requiredVersion("ktlint"))
                }
                kotlinGradle {
                    target(kotlinGradleFiles)
                    ktlint(versions.requiredVersion("ktlint"))
                }
            }
        }
}
