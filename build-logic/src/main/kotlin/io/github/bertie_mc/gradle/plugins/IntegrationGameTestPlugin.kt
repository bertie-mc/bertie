package io.github.bertie_mc.gradle.plugins

import io.github.bertie_mc.gradle.conventions.createTestCarrier
import io.github.bertie_mc.gradle.conventions.registerTestCarrier
import io.github.bertie_mc.gradle.conventions.requireTestReport
import io.github.bertie_mc.gradle.conventions.runTestsTask
import io.github.bertie_mc.gradle.conventions.useMinecraftTestExecutionSlot
import io.github.bertie_mc.gradle.model.MinecraftArtifactSide
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import java.time.Duration

/** A separate server fixture for optional integrations that require different installed mods. */
class IntegrationGameTestPlugin : Plugin<Project> {
    override fun apply(project: Project) =
        with(project) {
            pluginManager.withPlugin("bertie.gametest") {
                val carrier =
                    createTestCarrier(
                        sourceSetName = "integrationtest",
                        modIdSuffix = "integrationtests",
                        displayName = "Integration GameTests",
                        descriptionText = "Optional integration GameTests",
                        side = MinecraftArtifactSide.SERVER,
                    )
                dependencies.add(
                    carrier.sourceSet.runtimeOnlyConfigurationName,
                    dependencies.project(mapOf("path" to ":core:gametest-driver")),
                )
                val neoForge = extensions.getByType<NeoForgeExtension>()
                val loaded = registerTestCarrier(neoForge, carrier)
                val report = layout.buildDirectory.file("test-results/integrationtest/TEST-gametest.xml")
                neoForge.runs.register("integrationGameTests") {
                    type.set("gameTestServer")
                    sourceSet.set(carrier.sourceSet)
                    gameDirectory.set(layout.buildDirectory.dir("minecraft-runs/integrationtest"))
                    loadedMods.set(loaded)
                    systemProperties.put("neoforge.enabledGameTestNamespaces", carrier.subject.gameTestNamespace)
                    systemProperties.put("bertie.gametest.report", report.map { it.asFile.absolutePath })
                }
                tasks.named("runIntegrationGameTests") {
                    group = "verification"
                    description = "Runs optional integration GameTests in their own server fixture"
                    timeout.set(Duration.ofMinutes(15))
                    requireTestReport(report)
                }
                useMinecraftTestExecutionSlot("runIntegrationGameTests")
                tasks.named("runGameTests") { dependsOn("runIntegrationGameTests") }
                runTestsTask().configure { dependsOn("runIntegrationGameTests") }
            }
        }
}
