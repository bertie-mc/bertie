package io.github.bertie_mc.gradle.plugins

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

class ShadedModPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("bertie.mod") { configure(project) }
    }

    private fun configure(project: Project) = with(project) {
        val shadedLibrary = configurations.dependencyScope("shadedLibrary") {
            description = "Libraries merged into the production mod JAR"
        }
        val shadedClasspath = configurations.resolvable("shadedLibraryClasspath") {
            extendsFrom(shadedLibrary.get())
        }.get()
        configurations.named("compileOnly") { extendsFrom(shadedLibrary.get()) }
        val sourceSets = extensions.getByType<SourceSetContainer>()
        val main = sourceSets.getByName("main")
        val developmentLibraries = tasks.register<ShadowJar>("shadedDevelopmentLibraries") {
            archiveClassifier.set("development-libraries")
            configurations.set(listOf(shadedClasspath))
            configureShadedContents()
        }
        val developmentDirectory = layout.buildDirectory.dir("shaded-development-libraries")
        val prepareDevelopmentLibraries = tasks.register<Sync>("prepareShadedDevelopmentLibraries") {
            from(zipTree(developmentLibraries.flatMap { it.archiveFile }))
            into(developmentDirectory)
        }
        main.output.dir(
            mapOf("builtBy" to prepareDevelopmentLibraries),
            developmentDirectory,
        )

        tasks.named<Jar>("jar") { enabled = false }
        val shadedJar = tasks.register<ShadowJar>("shadowJar") {
            group = BasePlugin.BUILD_GROUP
            description = "Assembles the production mod JAR with its shaded libraries"
            from(main.output.classesDirs)
            from(tasks.named(main.processResourcesTaskName))
            from(layout.projectDirectory.file("NOTICE")) { into("META-INF") }
            from(layout.projectDirectory.file("UNLICENSE")) { into("META-INF") }
            archiveClassifier.set("")
            configurations.set(listOf(shadedClasspath))
            configureShadedContents()
        }

        listOf("apiElements", "runtimeElements").forEach { configurationName ->
            configurations.named(configurationName) {
                outgoing.artifacts.clear()
                outgoing.artifact(shadedJar)
            }
        }
        tasks.named("assemble") { dependsOn(shadedJar) }
    }

    private fun ShadowJar.configureShadedContents() {
        exclude(
            "META-INF/INDEX.LIST",
            "META-INF/MANIFEST.MF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
            "META-INF/*.SF",
            "META-INF/versions/*/module-info.class",
            "module-info.class",
        )
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
        filesNotMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.FAIL
        }
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}
