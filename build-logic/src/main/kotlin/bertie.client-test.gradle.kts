import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar

plugins {
    id("bertie.neoforge-mod")
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val mainSourceSet = sourceSets.named("main")
val clientTest = sourceSets.create("clientTest")

configurations.named(clientTest.implementationConfigurationName) {
    extendsFrom(configurations.getByName("implementation"))
}
configurations.named(clientTest.compileOnlyConfigurationName) {
    extendsFrom(configurations.getByName("compileOnly"))
}
configurations.named(clientTest.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.getByName("runtimeOnly"))
}

clientTest.compileClasspath += mainSourceSet.get().output
clientTest.runtimeClasspath += mainSourceSet.get().output

extensions.configure<NeoForgeExtension> {
    addModdingDependenciesTo(clientTest)
}

tasks.register<Jar>("clientTestJar") {
    group = "verification"
    description = "Build the test-only mod used by the headless client suite"
    archiveFileName = "${project.name}-client-tests.jar"
    destinationDirectory = layout.buildDirectory.dir("test-libs")
    from(clientTest.output)
    dependsOn(tasks.named(clientTest.classesTaskName))
}
