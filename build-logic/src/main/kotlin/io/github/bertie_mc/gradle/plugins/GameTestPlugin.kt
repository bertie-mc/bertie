package io.github.bertie_mc.gradle.plugins

import io.github.bertie_mc.gradle.conventions.createTestCarrier
import io.github.bertie_mc.gradle.conventions.registerTestCarrier
import io.github.bertie_mc.gradle.conventions.requireTestReport
import io.github.bertie_mc.gradle.conventions.runTestsTask
import io.github.bertie_mc.gradle.conventions.useMinecraftTestExecutionSlot
import io.github.bertie_mc.gradle.model.MinecraftArtifactSide
import io.github.bertie_mc.gradle.model.TestSubject
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import java.time.Duration

class GameTestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("bertie.mod") { configure(project) }
        project.pluginManager.withPlugin("bertie.pack") { configure(project) }
    }

    private fun configure(project: Project) = with(project) {
        val subject = extensions.getByType<TestSubject>()
        val carrier = createTestCarrier(
            sourceSetName = "gametest",
            modIdSuffix = "gametests",
            displayName = "${subject.id} GameTests",
            descriptionText = "GameTests for ${subject.id}",
            side = MinecraftArtifactSide.SERVER,
        )
        dependencies.add(
            carrier.sourceSet.runtimeOnlyConfigurationName,
            dependencies.project(mapOf("path" to ":core:gametest-driver")),
        )

        val neoForge = extensions.getByType<NeoForgeExtension>()
        val loadedMods = registerTestCarrier(neoForge, carrier)
        val runDirectory = layout.buildDirectory.dir("minecraft-runs/gametest")
        val report = layout.buildDirectory.file("test-results/gametest/TEST-gametest.xml")
        val prepareInstance = tasks.register<Sync>("prepareGameTestInstance") {
            into(runDirectory)
            from(layout.projectDirectory.dir("src/main/instance"))
            from(layout.projectDirectory.dir("src/gametest/instance"))
        }
        neoForge.runs.register("gameTests") {
            type.set("gameTestServer")
            sourceSet.set(carrier.sourceSet)
            gameDirectory.set(runDirectory)
            this.loadedMods.set(loadedMods)
            systemProperties.put(
                "neoforge.enabledGameTestNamespaces",
                carrier.subject.gameTestNamespace,
            )
            systemProperties.put(
                "bertie.gametest.report",
                report.map { it.asFile.absolutePath },
            )
            taskBefore(prepareInstance)
        }

        tasks.named("runGameTests") {
            group = "verification"
            description = "Runs the project's GameTests on a NeoForge dedicated server"
            timeout.set(Duration.ofMinutes(30))
            requireTestReport(report)
        }
        useMinecraftTestExecutionSlot("runGameTests")
        runTestsTask().configure { dependsOn("runGameTests") }
    }
}
