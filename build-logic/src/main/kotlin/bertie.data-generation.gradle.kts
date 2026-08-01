import io.github.bertie_mc.gradle.BertieModMetadata
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.tasks.SourceSetContainer

plugins {
    id("bertie.neoforge-mod")
}

val modMetadata = extensions.getByType<BertieModMetadata>()
val generatedResources = layout.projectDirectory.dir("src/generated/resources")

extensions.configure<NeoForgeExtension> {
    runs.register("data") {
        data()
        programArguments.addAll(
            "--mod",
            modMetadata.id,
            "--all",
            "--output",
            generatedResources.asFile.absolutePath,
            "--existing",
            layout.projectDirectory.dir("src/main/resources").asFile.absolutePath,
        )
    }
}

extensions.getByType<SourceSetContainer>().named("main") {
    resources.srcDir(generatedResources)
}
