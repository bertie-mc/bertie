pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        maven { url = uri("https://maven.neoforged.net/releases") }
    }
}

plugins {
    id("bertie.minecraft-artifacts")
}

rootProject.name = "bertie"

include(
    ":pack",
    ":testing:client-test-api",
    ":testing:client-test-driver",
    ":testing:gametest-driver",
    ":mods:berlords-food-system",
    ":mods:bertie-blackhole",
    ":mods:bertie-emi",
    ":mods:bertie-filters",
    ":mods:bertie-progression",
    ":mods:bertie-tiers",
    ":mods:bertie-weapons",
    ":mods:bush-tweaks",
    ":mods:berlords-carving",
    ":mods:config-migrations",
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
