package io.github.bertie_mc.gradle

import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.jvm.tasks.ProcessResources

/** Adds expanded `src/main/templates` metadata to the main resources. */
fun Project.registerModMetadataTemplates(
    replacementProperties: Map<String, String>,
): TaskProvider<ProcessResources> {
    val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
        inputs.properties(replacementProperties)
        expand(replacementProperties)
        from("src/main/templates")
        into(layout.buildDirectory.dir("generated/sources/modMetadata"))
    }

    extensions.getByType<SourceSetContainer>().named("main") {
        resources.srcDir(generateModMetadata)
    }
    extensions.getByType<NeoForgeExtension>().ideSyncTask(generateModMetadata)
    return generateModMetadata
}
