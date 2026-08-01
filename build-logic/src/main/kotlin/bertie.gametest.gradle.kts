import mc.bertie.gradle.BertieModMetadata
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.tasks.SourceSetContainer

plugins {
    id("bertie.neoforge-mod")
}

val modMetadata = extensions.getByType<BertieModMetadata>()
val sourceSets = extensions.getByType<SourceSetContainer>()
val mainSourceSet = sourceSets.named("main")
val gameTest = sourceSets.create("gameTest")

configurations.named(gameTest.implementationConfigurationName) {
    extendsFrom(configurations.getByName("implementation"))
}
configurations.named(gameTest.compileOnlyConfigurationName) {
    extendsFrom(configurations.getByName("compileOnly"))
}
configurations.named(gameTest.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.getByName("runtimeOnly"))
}

gameTest.compileClasspath += mainSourceSet.get().output
gameTest.runtimeClasspath += mainSourceSet.get().output

extensions.configure<NeoForgeExtension> {
    addModdingDependenciesTo(gameTest)
    mods.getByName(modMetadata.id).sourceSet(gameTest)

    runs.register("gameTestServer") {
        type = "gameTestServer"
        systemProperty("neoforge.enabledGameTestNamespaces", modMetadata.gameTestNamespace)
    }
}
