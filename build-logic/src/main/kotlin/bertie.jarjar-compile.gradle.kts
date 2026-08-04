import io.github.bertie_mc.gradle.tasks.ExtractNestedJars
import net.neoforged.moddevgradle.dsl.NeoForgeExtension

plugins {
    id("bertie.neoforge-mod")
}

val jarJarCompileOnly = configurations.dependencyScope("jarJarCompileOnly") {
    description = "Parent mods whose bundled libraries are needed for compilation"
}
val jarJarCompileClasspath = configurations.resolvable("jarJarCompileClasspath") {
    description = "Parent mod archives containing libraries needed for compilation"
    extendsFrom(jarJarCompileOnly.get())
    isTransitive = false
}

configurations.named("compileOnly") {
    extendsFrom(jarJarCompileOnly.get())
}

val extractedLibraries = layout.buildDirectory.dir("jarjar-compile-only")
val extractJarJarLibraries = tasks.register<ExtractNestedJars>("extractJarJarLibraries") {
    archives.from(jarJarCompileClasspath)
    destinationDirectory.set(extractedLibraries)
}

extensions.configure<NeoForgeExtension> {
    ideSyncTask(extractJarJarLibraries)
}

dependencies {
    compileOnly(
        fileTree(extractedLibraries) {
            include("*.jar")
        }.builtBy(extractJarJarLibraries),
    )
}
