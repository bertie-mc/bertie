import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.language.jvm.tasks.ProcessResources
import java.util.zip.ZipFile

@CacheableTask
abstract class ExtractJarJarLibraries : DefaultTask() {
    @get:Classpath
    abstract val archives: ConfigurableFileCollection

    @get:Input
    abstract val libraryNames: ListProperty<String>

    @get:OutputDirectory
    abstract val destinationDirectory: DirectoryProperty

    @TaskAction
    fun extract() {
        val destination = destinationDirectory.get().asFile
        destination.deleteRecursively()
        destination.mkdirs()
        val wanted = libraryNames.get()
        archives.files.forEach { parent ->
            ZipFile(parent).use { archive ->
                archive.entries().asSequence()
                    .filter { entry ->
                        entry.name.startsWith("META-INF/jarjar/") &&
                            entry.name.endsWith(".jar") &&
                            wanted.any(entry.name::contains)
                    }
                    .forEach { entry ->
                        val target = destination.resolve(entry.name.substringAfterLast('/'))
                        archive.getInputStream(entry).use { input ->
                            target.outputStream().use(input::copyTo)
                        }
                        logger.lifecycle("extractJarJarLibs: extracted ${target.name} from ${parent.name}")
                    }
            }
        }
    }
}

plugins {
    `java-library`
    idea
    id("net.neoforged.moddev") version "2.0.134"
}

val minecraft_version: String by project
val minecraft_version_range: String by project
val neo_version: String by project
val neo_version_range: String by project
val loader_version_range: String by project
val parchment_mappings_version: String by project
val parchment_minecraft_version: String by project
val mod_id: String by project
val mod_name: String by project
val mod_license: String by project
val mod_version: String by project
val mod_group_id: String by project
val mod_authors: String by project
val mod_description: String by project

version = mod_version
group = mod_group_id

repositories {
    mavenLocal()
    maven { url = uri("https://api.modrinth.com/maven") }

    // bertie's own mods, resolved straight from their GitHub Releases. The release
    // asset is named <module>-<version>.jar and the tag is v<version>, which maps
    // onto an ivy layout cleanly. Keeps the build reproducible off this machine
    // without vendoring a jar into libs/.
    ivy {
        url = uri("https://github.com/bertie-mc")
        patternLayout {
            artifact("/[organisation]/releases/download/v[revision]/[module]-[revision].jar")
        }
        metadataSources { artifact() }
        content { includeGroup("bertie-progression") }
    }
}

base {
    archivesName = mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = neo_version

    parchment {
        mappingsVersion = parchment_mappings_version
        minecraftVersion = parchment_minecraft_version
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
        register(mod_id) {
            sourceSet(sourceSets.main.get())
        }
    }
}

// A few integration libraries emi compiles against (anvillib 1.4.0, l2core, l2serial, confluence_magic_lib)
// are NOT published standalone on any maven — they ship JarJar-embedded inside their parent mods. We pull the
// parent mods from Modrinth and extract just those nested jars at build time. Nothing third-party is committed.
val jarJarParents by configurations.creating
val extractedLibsDir = layout.buildDirectory.dir("extracted-jarjar-libs").get().asFile

val extractJarJarLibs = tasks.register<ExtractJarJarLibraries>("extractJarJarLibs") {
    description = "Extract JarJar-embedded libraries from parent mods onto the compile classpath."
    archives.from(jarJarParents)
    libraryNames.set(
        listOf(
            "anvillib-neoforge-1.21.1-1.4.0",
            "l2core-3.0.8",
            "l2serial-3.0.9",
            "org.confluence.lib.confluence_magic_lib",
        ),
    )
    destinationDirectory.set(layout.buildDirectory.dir("extracted-jarjar-libs"))
}

dependencies {
    // Integration APIs, resolved from Modrinth (runtime-optional, ModList-guarded; compileOnly = symbols only).
    compileOnly("maven.modrinth:emi:1.1.24+1.21.1+neoforge")
    compileOnly("maven.modrinth:slag-n-embers:1.1a")
    compileOnly("maven.modrinth:create:6.0.10+mc1.21.1")
    compileOnly("maven.modrinth:l_enders-cataclysm:3.31")
    compileOnly("maven.modrinth:irons-spells-n-spellbooks:1.21.1-3.16.0")
    compileOnly("maven.modrinth:forbidden-arcanus:2.6.1")
    compileOnly("maven.modrinth:terra-curio:1.1.1")
    compileOnly("maven.modrinth:enderio:v8.2.11-beta")
    compileOnly("maven.modrinth:malum:1.8.2")
    compileOnly("maven.modrinth:lodestonelib:1.8.2") // Malum recipe base (LodestoneInWorldRecipe)
    compileOnly("maven.modrinth:l2library:3.0.8")
    compileOnly("maven.modrinth:extradelight:2.6.5")
    compileOnly("maven.modrinth:dungeons_delight:1.5.0")
    compileOnly("maven.modrinth:expanded-delight:0.1.4-neoforge")
    compileOnly("maven.modrinth:avaritias-delight:1.6.3")
    compileOnly("maven.modrinth:farmers-pizzeria:1.1.0")
    compileOnly("maven.modrinth:farmers-delight:1.21.1-1.3.2") // Delight-addon base classes
    compileOnly("maven.modrinth:gensokyo-delight-youkais-feasts:1.1.0")
    compileOnly("maven.modrinth:cognition:2.4.12")
    compileOnly("maven.modrinth:stellaris:3OXCvg6r") // pin NeoForge version by id (1.4.23; a Fabric build shares the number)
    compileOnly("maven.modrinth:twilight-delight:3.2.2")
    compileOnly("maven.modrinth:slavic-delight:0.3.2")
    compileOnly("maven.modrinth:cuisine-delight:1.2.8")
    compileOnly("maven.modrinth:berries-and-cherries:1.1")
    compileOnly("maven.modrinth:better-archeology:rp4lPDKI") // pin NeoForge version by id (1.21.1-1.3.4)
    compileOnly("maven.modrinth:l2-complements:3.1.3")
    compileOnly("maven.modrinth:anvilcraft:1.21.1-1.5.3+hotfix.1849") // dev.dubhe.anvilcraft.recipe.*
    // Advanced Loot Info: its plugin API (com.yanny.ali.api.* + bundled com.yanny.aci.*) lets us publish
    // Malum's hardcoded soul-reaping drops onto ALI's own mob-drop pages. Pinned by version id (1.21.1-1.12.0).
    compileOnly("maven.modrinth:advanced-loot-info:y8p1Vq83")

    // Bertie Progression: BedRecipes data for the Mallet Work category. Pulled from its GitHub
    // Release (see the ivy repo above). This used to read the sibling project's
    // build/libs output directly, which meant the build only worked on a machine that
    // had just built Bertie Progression by hand - CI could not compile this module at all.
    compileOnly("bertie-progression:bertie-progression:0.25.1")

    // Parent mods we only need for their JarJar-embedded libs (extracted by extractJarJarLibs, below):
    add(jarJarParents.name, "maven.modrinth:anvilcraft:1.21.1-1.5.3+hotfix.1849") // -> anvillib 1.4.0
    add(jarJarParents.name, "maven.modrinth:gensokyo-delight-youkais-feasts:1.1.0") // -> l2core + l2serial
    add(jarJarParents.name, "maven.modrinth:terra-curio:1.1.1") // -> confluence_magic_lib

    // The nested libs extracted above (produced before compileJava):
    compileOnly(fileTree(extractedLibsDir) { include("*.jar") }.builtBy(extractJarJarLibs))
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraft_version,
        "minecraft_version_range" to minecraft_version_range,
        "neo_version" to neo_version,
        "neo_version_range" to neo_version_range,
        "loader_version_range" to loader_version_range,
        "mod_id" to mod_id,
        "mod_name" to mod_name,
        "mod_license" to mod_license,
        "mod_version" to mod_version,
        "mod_authors" to mod_authors,
        "mod_description" to mod_description,
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into("build/generated/sources/modMetadata")
}
sourceSets.main.get().resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)
