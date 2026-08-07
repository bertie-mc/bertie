package io.github.bertie_mc.gradle.conventions

import io.github.bertie_mc.gradle.model.MinecraftArtifactSide
import io.github.bertie_mc.gradle.model.PlatformVersions
import io.github.bertie_mc.gradle.model.TestSubject
import io.github.bertie_mc.gradle.tasks.GenerateSuiteModMetadata
import net.neoforged.moddevgradle.dsl.ModModel
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

internal data class TestCarrier(
    val sourceSet: SourceSet,
    val modId: String,
    val subject: TestSubject,
)

internal fun Project.createTestCarrier(
    sourceSetName: String,
    modIdSuffix: String,
    displayName: String,
    descriptionText: String,
    side: MinecraftArtifactSide,
): TestCarrier {
    val sourceSets = extensions.getByType<SourceSetContainer>()
    val main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
    val sourceSet = sourceSets.create(sourceSetName)

    configurations.getByName(sourceSet.implementationConfigurationName).extendsFrom(
        configurations.getByName(main.implementationConfigurationName),
    )
    configurations.getByName(sourceSet.compileOnlyConfigurationName).extendsFrom(
        configurations.getByName(main.compileOnlyConfigurationName),
    )
    configurations.getByName(sourceSet.runtimeOnlyConfigurationName).extendsFrom(
        configurations.getByName(main.runtimeOnlyConfigurationName),
    )
    sourceSet.compileClasspath += main.output
    sourceSet.runtimeClasspath += main.output
    projectMinecraftRuntime(
        configurations.getByName(sourceSet.runtimeClasspathConfigurationName),
        side,
    )

    val subject = extensions.getByType<TestSubject>()
    val carrierId = "${subject.id}_$modIdSuffix"
    val platform = extensions.getByType<PlatformVersions>()
    val metadata =
        tasks.register<GenerateSuiteModMetadata>(
            "generate${sourceSetName.replaceFirstChar(Char::uppercaseChar)}Metadata",
        ) {
            modId.set(carrierId)
            this.displayName.set(displayName)
            this.descriptionText.set(descriptionText)
            license.set(subject.license)
            minecraftVersionRange.set(platform.minecraftVersionRange)
            neoForgeVersionRange.set(platform.neoForgeVersionRange)
            javaFmlLoaderVersionRange.set(platform.javaFmlLoaderVersionRange)
            subject.testedModId?.let(testedModId::set)
            outputDirectory.set(layout.buildDirectory.dir("generated/$sourceSetName/metadata"))
        }
    sourceSet.resources.srcDir(metadata)

    return TestCarrier(sourceSet, carrierId, subject)
}

internal fun Project.registerTestCarrier(
    neoForge: NeoForgeExtension,
    carrier: TestCarrier,
): Set<ModModel> {
    neoForge.addModdingDependenciesTo(carrier.sourceSet)
    val carrierMod =
        neoForge.mods
            .register(carrier.modId) {
                sourceSet(carrier.sourceSet)
            }.get()
    return buildSet {
        carrier.subject.testedModId?.let { add(neoForge.mods.getByName(it)) }
        add(carrierMod)
    }
}

internal fun Project.runTestsTask(): TaskProvider<Task> = tasks.named("runTests")

internal fun Task.requireTestReport(report: Provider<RegularFile>) {
    doFirst { prepareTestReport(report.get().asFile) }
    doLast { validateTestReport(report.get().asFile) }
}

private fun prepareTestReport(report: File) {
    report.delete()
    report.parentFile.mkdirs()
}

private fun validateTestReport(report: File) {
    require(report.isFile && report.length() > 0) {
        "Test process completed without producing ${report.absolutePath}"
    }

    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report)
    val testCases = document.getElementsByTagName("testcase").length
    val declaredTests =
        document.documentElement
            .getAttribute("tests")
            .takeIf(String::isNotBlank)
            ?.toIntOrNull()
    require((declaredTests ?: testCases) > 0) {
        "Test report ${report.absolutePath} contains no tests"
    }

    val failures = document.getElementsByTagName("failure").length
    val errors = document.getElementsByTagName("error").length
    require(failures == 0 && errors == 0) {
        "Test report ${report.absolutePath} contains $failures failures and $errors errors"
    }
}
