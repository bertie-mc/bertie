package io.github.bertie_mc.gradle.plugins

import io.github.bertie_mc.gradle.conventions.configureJvmRole
import io.github.bertie_mc.gradle.conventions.configureNeoForgeRole
import io.github.bertie_mc.gradle.model.ModMetadata
import io.github.bertie_mc.gradle.model.TestSubject
import io.github.bertie_mc.gradle.model.platformVersions
import io.github.bertie_mc.gradle.tasks.GenerateModMetadata
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

class ModPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            configureJvmRole()
            val neoForge = configureNeoForgeRole()

            val metadata = ModMetadata.parse(
                providers.fileContents(layout.projectDirectory.file("mod.properties")).asText.get(),
            )
            extensions.add(ModMetadata::class.java, "bertieMod", metadata)
            extensions.add(
                TestSubject::class.java,
                "bertieTestSubject",
                TestSubject(
                    id = metadata.id,
                    license = metadata.license,
                    gameTestNamespace = metadata.gameTestNamespace,
                    testedModId = metadata.id,
                ),
            )

            version = metadata.version
            group = metadata.group
            extensions.getByType<BasePluginExtension>().archivesName.set(metadata.archiveName)

            val platform = extensions.getByType<VersionCatalogsExtension>()
                .named("libs")
                .platformVersions()
            val generateModMetadata = tasks.register<GenerateModMetadata>("generateModMetadata") {
                templateDirectory.set(layout.projectDirectory.dir("src/main/templates"))
                replacements.set(metadata.templateProperties(platform))
                outputDirectory.set(layout.buildDirectory.dir("generated/modMetadata"))
            }
            extensions.getByType<SourceSetContainer>().named("main") {
                resources.srcDir(generateModMetadata)
            }

            val main = extensions.getByType<SourceSetContainer>().getByName("main")
            neoForge.mods.register(metadata.id) {
                sourceSet(main)
            }
            val accessTransformer =
                layout.projectDirectory.file("src/main/resources/META-INF/accesstransformer.cfg")
            if (accessTransformer.asFile.isFile) {
                neoForge.accessTransformers.from(accessTransformer)
            }

            tasks.named<Jar>("jar") {
                from(layout.projectDirectory.file("NOTICE")) { into("META-INF") }
                from(layout.projectDirectory.file("UNLICENSE")) { into("META-INF") }
            }
        }
    }
}
