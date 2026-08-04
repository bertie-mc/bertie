import io.github.bertie_mc.gradle.BertieModMetadata
import io.github.bertie_mc.gradle.BertiePlatformVersions
import io.github.bertie_mc.gradle.BertieTestingExtension
import io.github.bertie_mc.gradle.MinecraftArtifactSide
import io.github.bertie_mc.gradle.gameTestCarrierId
import io.github.bertie_mc.gradle.projectMinecraftRuntime
import io.github.bertie_mc.gradle.requireTestReport
import io.github.bertie_mc.gradle.runTestsTask
import io.github.bertie_mc.gradle.tasks.GenerateSuiteModMetadata
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync

plugins {
    id("bertie.neoforge-base")
}

val platformVersions = extensions.getByType<BertiePlatformVersions>()
val testing = extensions.getByType<BertieTestingExtension>()
val sourceSets = extensions.getByType<SourceSetContainer>()
val mainSourceSet = sourceSets.named("main")
val gametest = sourceSets.create("gametest")

configurations.named(gametest.implementationConfigurationName) {
    extendsFrom(configurations.getByName("implementation"))
}
configurations.named(gametest.compileOnlyConfigurationName) {
    extendsFrom(configurations.getByName("compileOnly"))
}
configurations.named(gametest.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.getByName("runtimeOnly"))
}

gametest.compileClasspath += mainSourceSet.get().output
gametest.runtimeClasspath += mainSourceSet.get().output
projectMinecraftRuntime(gametest, MinecraftArtifactSide.SERVER)

dependencies {
    add(gametest.runtimeOnlyConfigurationName, project(":testing:gametest-driver"))
}

val carrierId = gameTestCarrierId(testing.subjectId.get())
val generateGametestMetadata = tasks.register<GenerateSuiteModMetadata>("generateGametestMetadata") {
    modId.set(carrierId)
    displayName.set(testing.subjectId.map { "$it GameTests" })
    descriptionText.set(testing.subjectId.map { "Test-only GameTests for $it" })
    license.set(testing.license)
    minecraftVersionRange.set(platformVersions.minecraftVersionRange)
    neoForgeVersionRange.set(platformVersions.neoForgeVersionRange)
    javaFmlLoaderVersionRange.set(platformVersions.javaFmlLoaderVersionRange)
    outputDirectory.set(layout.buildDirectory.dir("generated/sources/gametestMetadata"))
}
pluginManager.withPlugin("bertie.neoforge-mod") {
    generateGametestMetadata.configure {
        testedModId.set(project.extensions.getByType<BertieModMetadata>().id)
    }
}
gametest.resources.srcDir(generateGametestMetadata)

val gameTestRunDirectory = layout.buildDirectory.dir("runs/gametest")
val gameTestReport = layout.buildDirectory.file("test-results/gametest/TEST-gametest.xml")
val prepareGametestInstance = tasks.register<Sync>("prepareGametestInstance") {
    group = "verification"
    description = "Stages the isolated GameTest instance"
    into(gameTestRunDirectory)
    from(testing.mainInstanceDirectory)
    from(layout.projectDirectory.dir("src/gametest/instance"))
}

extensions.configure<NeoForgeExtension> {
    addModdingDependenciesTo(gametest)
    mods.register(carrierId) {
        sourceSet(gametest)
    }

    runs.register("gameTests") {
        type = "gameTestServer"
        sourceSet.set(gametest)
        gameDirectory.set(gameTestRunDirectory)
        systemProperties.put("neoforge.enabledGameTestNamespaces", testing.gameTestNamespace)
        systemProperties.put(
            "bertie.gametest.report",
            gameTestReport.map { it.asFile.absolutePath },
        )
        taskBefore(prepareGametestInstance)
    }
}

tasks.named("runGameTests") {
    group = "verification"
    description = "Runs the project's GameTests on a NeoForge GameTest server"
    requireTestReport(gameTestReport)
}

runTestsTask().configure {
    dependsOn(tasks.named("runGameTests"))
}
