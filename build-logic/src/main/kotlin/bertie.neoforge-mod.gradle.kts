import io.github.bertie_mc.gradle.BertieModMetadata
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
    idea
    id("net.neoforged.moddev")
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun catalogVersion(name: String): String =
    versionCatalog.findVersion(name).orElseThrow {
        IllegalStateException("Version '$name' is missing from the root version catalog")
    }.requiredVersion

val modMetadataFile = layout.projectDirectory.file("mod.properties")
val accessTransformerFile = layout.projectDirectory.file("src/main/resources/META-INF/accesstransformer.cfg")
val modMetadata = BertieModMetadata.parse(
    providers.fileContents(modMetadataFile).asText.get(),
    catalogVersion("neoforge"),
)
extensions.add("bertieMod", modMetadata)

version = modMetadata.version
group = modMetadata.group

repositories {
    mavenCentral()
    maven {
        name = "NeoForge"
        url = uri("https://maven.neoforged.net/releases")
        content { includeGroupByRegex("net\\.neoforged(\\..*)?") }
    }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content { includeGroup("maven.modrinth") }
    }
    maven {
        name = "Create"
        url = uri("https://maven.createmod.net")
        content {
            includeGroup("com.simibubi.create")
            includeGroup("net.createmod.ponder")
            includeGroup("dev.engine-room.flywheel")
        }
    }
    maven {
        name = "Architectury"
        url = uri("https://maven.architectury.dev")
        content { includeGroup("dev.architectury") }
    }
    maven {
        name = "FTB"
        url = uri("https://maven.ftb.dev/releases")
        content { includeGroup("dev.ftb.mods") }
    }
}

base {
    archivesName = modMetadata.archiveName
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = modMetadata.neoForgeVersion

    if (accessTransformerFile.asFile.isFile) {
        accessTransformers.from(accessTransformerFile)
    }

    parchment {
        minecraftVersion = catalogVersion("parchment-minecraft")
        mappingsVersion = catalogVersion("parchment-mappings")
    }

    runs {
        register("client") {
            client()
        }
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        register(modMetadata.id) {
            sourceSet(sourceSets.main.get())
        }
    }
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = modMetadata.templateProperties(catalogVersion("minecraft"))
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

sourceSets.main.get().resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)
