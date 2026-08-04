import io.github.bertie_mc.gradle.BertieModMetadata
import io.github.bertie_mc.gradle.BertiePlatformVersions
import io.github.bertie_mc.gradle.BertieTestingExtension
import io.github.bertie_mc.gradle.MinecraftArtifactSide
import io.github.bertie_mc.gradle.projectMinecraftRuntime
import io.github.bertie_mc.gradle.registerModMetadataTemplates
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.tasks.SourceSetContainer

plugins {
    id("bertie.neoforge-base")
}

val platformVersions = extensions.getByType<BertiePlatformVersions>()
val sourceSets = extensions.getByType<SourceSetContainer>()
val mainSourceSet = sourceSets.named("main")

projectMinecraftRuntime(mainSourceSet.get(), MinecraftArtifactSide.CLIENT)

val modMetadataFile = layout.projectDirectory.file("mod.properties")
val accessTransformerFile = layout.projectDirectory.file("src/main/resources/META-INF/accesstransformer.cfg")
val modMetadata = BertieModMetadata.parse(
    providers.fileContents(modMetadataFile).asText.get(),
)
extensions.add("bertieMod", modMetadata)

version = modMetadata.version
group = modMetadata.group

base {
    archivesName = modMetadata.archiveName
}

extensions.configure<NeoForgeExtension> {
    if (accessTransformerFile.asFile.isFile) {
        accessTransformers.from(accessTransformerFile)
    }

    runs {
        register("client") {
            client()
            sourceSet.set(mainSourceSet)
        }
    }

    mods {
        register(modMetadata.id) {
            sourceSet(mainSourceSet.get())
        }
    }
}

extensions.getByType<BertieTestingExtension>().apply {
    subjectId.set(modMetadata.id)
    gameTestNamespace.set(modMetadata.gameTestNamespace)
    license.set(modMetadata.license)
}

registerModMetadataTemplates(modMetadata.templateProperties(platformVersions))
