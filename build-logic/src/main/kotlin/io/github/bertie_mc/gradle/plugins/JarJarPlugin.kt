package io.github.bertie_mc.gradle.plugins

import io.github.bertie_mc.gradle.tasks.ExtractNestedJars
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class JarJarPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("bertie.mod") { configure(project) }
    }

    private fun configure(project: Project) =
        with(project) {
            val dependenciesToExtract =
                configurations.dependencyScope("jarJarCompileOnly") {
                    description = "Mods whose nested libraries are needed for compilation"
                }
            val archives =
                configurations
                    .resolvable("jarJarCompileClasspath") {
                        extendsFrom(dependenciesToExtract.get())
                        isTransitive = false
                    }.get()
            configurations.named("compileOnly") {
                extendsFrom(dependenciesToExtract.get())
            }
            val destination = layout.buildDirectory.dir("jarjar-compile")
            val extract =
                tasks.register<ExtractNestedJars>("extractJarJarLibraries") {
                    this.archives.from(archives)
                    destinationDirectory.set(destination)
                }
            dependencies.add(
                "compileOnly",
                fileTree(destination) { include("*.jar") }.builtBy(extract),
            )
        }
}
