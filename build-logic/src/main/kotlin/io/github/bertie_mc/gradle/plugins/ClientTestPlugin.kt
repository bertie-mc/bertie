package io.github.bertie_mc.gradle.plugins

import io.github.bertie_mc.gradle.conventions.addArm64LwjglNatives
import io.github.bertie_mc.gradle.conventions.createTestCarrier
import io.github.bertie_mc.gradle.conventions.registerTestCarrier
import io.github.bertie_mc.gradle.conventions.requireTestReport
import io.github.bertie_mc.gradle.conventions.runTestsTask
import io.github.bertie_mc.gradle.model.MinecraftArtifactSide
import io.github.bertie_mc.gradle.model.TestSubject
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

class ClientTestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("bertie.mod") { configure(project) }
        project.pluginManager.withPlugin("bertie.pack") { configure(project) }
    }

    private fun configure(project: Project) = with(project) {
        val subject = extensions.getByType<TestSubject>()
        val carrier = createTestCarrier(
            sourceSetName = "clienttest",
            modIdSuffix = "clienttests",
            displayName = "${subject.id} client tests",
            descriptionText = "Client tests for ${subject.id}",
            side = MinecraftArtifactSide.CLIENT,
        )
        dependencies.add(
            carrier.sourceSet.implementationConfigurationName,
            dependencies.project(mapOf("path" to ":core:client-test-api")),
        )
        dependencies.add(
            carrier.sourceSet.runtimeOnlyConfigurationName,
            dependencies.project(mapOf("path" to ":core:client-test-driver")),
        )

        val neoForge = extensions.getByType<NeoForgeExtension>()
        val loadedMods = registerTestCarrier(neoForge, carrier)
        val runDirectory = layout.buildDirectory.dir("minecraft-runs/clienttest")
        val report = layout.buildDirectory.file("test-results/clienttest/TEST-clienttest.xml")
        val diagnostics = layout.buildDirectory.dir("test-diagnostics/clienttest")
        val prepareInstance = tasks.register<Sync>("prepareClientTestInstance") {
            into(runDirectory)
            from(layout.projectDirectory.dir("src/main/instance"))
            from(layout.projectDirectory.dir("src/clienttest/instance"))
        }
        neoForge.runs.register("clientTests") {
            client()
            sourceSet.set(carrier.sourceSet)
            gameDirectory.set(runDirectory)
            this.loadedMods.set(loadedMods)
            systemProperties.put(
                "bertie.clienttest.report",
                report.map { it.asFile.absolutePath },
            )
            systemProperties.put(
                "bertie.clienttest.diagnostics",
                diagnostics.map { it.asFile.absolutePath },
            )
            taskBefore(prepareInstance)
            addArm64LwjglNatives(this)
        }

        tasks.named("runClientTests") {
            group = "verification"
            description = "Runs the project's annotated client tests in Minecraft"
            requireTestReport(report)
        }
        runTestsTask().configure { dependsOn("runClientTests") }
    }
}
