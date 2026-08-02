pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        maven { url = uri("https://maven.neoforged.net/releases") }
    }
}

rootProject.name = "bertie"

include(
    ":mods:berlords-food-system",
    ":mods:bertie-blackhole",
    ":mods:bertie-emi",
    ":mods:bertie-filters",
    ":mods:bertie-progression",
    ":mods:bertie-tiers",
    ":mods:bertie-weapons",
    ":mods:bush-tweaks",
    ":mods:berlords-carving",
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
    ":mods:short-circuit-fix",
    ":mods:withered-hearts",
)
