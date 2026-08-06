import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        maven { url = uri("https://maven.neoforged.net/releases") }
    }
}

plugins {
    id("bertie.settings")
}

rootProject.name = "bertie"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        val central = mavenCentral()
        exclusiveContent {
            forRepositories(central)
            filter { includeGroup("org.lwjgl") }
        }
        maven {
            name = "Parchment"
            url = uri("https://maven.parchmentmc.org")
            content { includeGroup("org.parchmentmc.data") }
        }
        maven {
            name = "Modrinth"
            url = uri("https://api.modrinth.com/maven")
            content { includeGroup("maven.modrinth") }
            metadataSources { artifact() }
        }
        maven {
            name = "CurseMaven"
            url = uri("https://www.cursemaven.com")
            content { includeGroup("curse.maven") }
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
}

include(
    ":core:minecraft",
    ":pack",
    ":core:client-test-api",
    ":core:client-test-driver",
    ":core:gametest-driver",
    ":mods:alexscaves-worldgen-fix",
    ":mods:berlords-food-system",
    ":mods:bertie-blackhole",
    ":mods:bertie-emi",
    ":mods:bertie-filters",
    ":mods:bertie-progression",
    ":mods:bertie-tiers",
    ":mods:bertie-weapons",
    ":mods:bush-tweaks",
    ":mods:berlords-carving",
    ":mods:cataclysm-fortresses",
    ":mods:config-migrations",
    ":mods:cultural-delights-fix",
    ":mods:ender-eyes",
    ":mods:explode-to-mine",
    ":mods:explosive-enhancement",
    ":mods:fart-bomb",
    ":mods:fd-shader-fix",
    ":mods:forge-ink",
    ":mods:frozen-reg-fix",
    ":mods:hephaestus-architecture",
    ":mods:primitive-refined",
    ":mods:rustic-engineer-fix",
    ":mods:screenshot-copy",
    ":mods:short-circuit-fix",
    ":mods:withered-hearts",
)
