import io.github.bertie_mc.gradle.MinecraftArtifactSide
import io.github.bertie_mc.gradle.projectMinecraftRuntime
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.tasks.SourceSetContainer

plugins {
    id("bertie.neoforge-mod")
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val mainSourceSet = sourceSets.named("main")
// This source set carries only the dedicated-server runtime; sources stay in src/main.
val serverRuntimeSourceSet = sourceSets.create("serverruntime") {
    java.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
}

configurations.named(serverRuntimeSourceSet.implementationConfigurationName) {
    extendsFrom(configurations.getByName(mainSourceSet.get().implementationConfigurationName))
}
configurations.named(serverRuntimeSourceSet.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.getByName(mainSourceSet.get().runtimeOnlyConfigurationName))
}
serverRuntimeSourceSet.runtimeClasspath += mainSourceSet.get().output
projectMinecraftRuntime(serverRuntimeSourceSet, MinecraftArtifactSide.SERVER)

extensions.configure<NeoForgeExtension> {
    addModdingDependenciesTo(serverRuntimeSourceSet)

    runs.register("server") {
        server()
        sourceSet.set(serverRuntimeSourceSet)
        programArgument("--nogui")
    }
}
