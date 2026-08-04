import io.github.bertie_mc.gradle.BertieModMetadata
import io.github.bertie_mc.gradle.BertiePlatformVersions
import io.github.bertie_mc.gradle.BertieTestingExtension
import io.github.bertie_mc.gradle.MinecraftArtifactSide
import io.github.bertie_mc.gradle.clientTestCarrierId
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
val clienttest = sourceSets.create("clienttest")

configurations.named(clienttest.implementationConfigurationName) {
    extendsFrom(configurations.getByName("implementation"))
}
configurations.named(clienttest.compileOnlyConfigurationName) {
    extendsFrom(configurations.getByName("compileOnly"))
}
configurations.named(clienttest.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.getByName("runtimeOnly"))
}

clienttest.compileClasspath += mainSourceSet.get().output
clienttest.runtimeClasspath += mainSourceSet.get().output
projectMinecraftRuntime(clienttest, MinecraftArtifactSide.CLIENT)

dependencies {
    add(clienttest.implementationConfigurationName, project(":testing:client-test-api"))
    add(clienttest.runtimeOnlyConfigurationName, project(":testing:client-test-driver"))
}

val carrierId = clientTestCarrierId(testing.subjectId.get())
val generateClienttestMetadata = tasks.register<GenerateSuiteModMetadata>("generateClienttestMetadata") {
    modId.set(carrierId)
    displayName.set(testing.subjectId.map { "$it client tests" })
    descriptionText.set(testing.subjectId.map { "Test-only client assertions for $it" })
    license.set(testing.license)
    minecraftVersionRange.set(platformVersions.minecraftVersionRange)
    neoForgeVersionRange.set(platformVersions.neoForgeVersionRange)
    javaFmlLoaderVersionRange.set(platformVersions.javaFmlLoaderVersionRange)
    outputDirectory.set(layout.buildDirectory.dir("generated/sources/clienttestMetadata"))
}
pluginManager.withPlugin("bertie.neoforge-mod") {
    generateClienttestMetadata.configure {
        testedModId.set(project.extensions.getByType<BertieModMetadata>().id)
    }
}
clienttest.resources.srcDir(generateClienttestMetadata)

val clientTestRunDirectory = layout.buildDirectory.dir("runs/clienttest")
val clientTestReport = layout.buildDirectory.file("test-results/clienttest/TEST-clienttest.xml")
val clientTestDiagnostics = layout.buildDirectory.dir("test-diagnostics/clienttest")
val prepareClienttestInstance = tasks.register<Sync>("prepareClienttestInstance") {
    group = "verification"
    description = "Stages the isolated client-test instance"
    into(clientTestRunDirectory)
    from(testing.mainInstanceDirectory)
    from(layout.projectDirectory.dir("src/clienttest/instance"))
}
extensions.configure<NeoForgeExtension> {
    addModdingDependenciesTo(clienttest)
    mods.register(carrierId) {
        sourceSet(clienttest)
    }

    runs.register("clientTests") {
        client()
        sourceSet.set(clienttest)
        gameDirectory.set(clientTestRunDirectory)
        systemProperties.put(
            "bertie.clienttest.report",
            clientTestReport.map { it.asFile.absolutePath },
        )
        systemProperties.put(
            "bertie.clienttest.diagnostics",
            clientTestDiagnostics.map { it.asFile.absolutePath },
        )
        taskBefore(prepareClienttestInstance)
    }
}

tasks.named("runClientTests") {
    group = "verification"
    description = "Runs the project's annotated client tests in Minecraft"
    requireTestReport(clientTestReport)
}

runTestsTask().configure {
    dependsOn(tasks.named("runClientTests"))
}
