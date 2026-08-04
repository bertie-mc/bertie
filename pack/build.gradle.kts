import io.github.bertie_mc.gradle.packRuntime

plugins {
    id("bertie.pack")
    id("bertie.gametest")
    id("bertie.client-test")
}

dependencies {
    packRuntime(
        project(":mods:berlords-carving"),
        project(":mods:berlords-food-system"),
        project(":mods:bertie-blackhole"),
        project(":mods:bertie-emi"),
        project(":mods:bertie-filters"),
        project(":mods:bertie-progression"),
        project(":mods:bertie-tiers"),
        project(":mods:bertie-weapons"),
        project(":mods:bush-tweaks"),
        project(":mods:config-migrations"),
        project(":mods:ender-eyes"),
        project(":mods:explode-to-mine"),
        project(":mods:fart-bomb"),
        project(":mods:forge-ink"),
        project(":mods:frozen-reg-fix"),
        project(":mods:hephaestus-architecture"),
        project(":mods:primitive-refined"),
        project(":mods:rustic-engineer-fix"),
        project(":mods:explosive-enhancement"),
        project(":mods:fd-shader-fix"),
        project(":mods:screenshot-copy"),
        project(":mods:short-circuit-fix"),
        project(":mods:withered-hearts"),
    )
}
