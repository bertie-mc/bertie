import org.gradle.api.tasks.testing.Test

plugins {
    id("bertie.mod")
    id("bertie.jarjar")
    id("bertie.neoforge-test")
    id("bertie.gametest")
    id("bertie.integration-gametest")
    id("bertie.dev-runs")
}

tasks.named<Test>("test") {
    maxHeapSize = "2G"
}

// Unrelated pack rituals refer to optional mods absent from this focused server fixture.
// Retain their registry keys without installing the complete pack to test two selected rituals.
val ritualSources =
    fileTree("src/main/resources/data") {
        include("**/forbidden_arcanus/hephaestus_forge/ritual/**/*.json")
    }
val ritualSourceRoot = file("src/main/resources/data")
val isolatedRitualDirectory = layout.buildDirectory.dir("generated/integration-rituals")
val isolateIntegrationRituals by tasks.registering {
    inputs.files(ritualSources)
    outputs.dir(isolatedRitualDirectory)
    val sourceFiles = ritualSources.files.toList()
    val sourceRoot = ritualSourceRoot
    val outputRoot = isolatedRitualDirectory.get().asFile
    doLast {
        val selected =
            setOf(
                "forbidden_arcanus/forbidden_arcanus/hephaestus_forge/ritual/upgrade_tier_3.json",
                "bertieprogression/forbidden_arcanus/hephaestus_forge/ritual/machine_casing.json",
            )
        sourceFiles.forEach { source ->
            val relative = source.relativeTo(sourceRoot).invariantSeparatorsPath
            if (relative !in selected) {
                val target = outputRoot.resolve("data/$relative")
                target.parentFile.mkdirs()
                target.writeText(
                    """{"essences":{},"inputs":[{"amount":1,"ingredient":{"item":"minecraft:cobblestone"}}],"main_ingredient":{"item":"minecraft:stone"},"magic_circle":"forbidden_arcanus:create_item","result":{"type":"forbidden_arcanus:create_item","result_item":{"id":"minecraft:stone"}}}""",
                )
            }
        }
    }
}
sourceSets.named("integrationtest") {
    resources.srcDir(isolateIntegrationRituals)
}

dependencies {
    compileOnly(deps.emi)
    compileOnly(deps.advancedLootInfo)
    compileOnly(deps.apotheosis)
    compileOnly(deps.enigmaticlegacy)
    compileOnly(deps.universalSawmill)
    compileOnly(deps.pastelMod)
    compileOnly(deps.mekanism)
    jarJarCompileOnly(deps.l2Complements)
    jarJarCompileOnly(deps.anvilcraft)
    jarJarCompileOnly(deps.terraCurio)
    compileOnly(deps.ironsSpellsNSpellbooks)
    compileOnly(deps.simplySwords)
    runtimeOnly(deps.patchouli)

    // Ponder is published directly by Create's Maven; use the slim Create artifact so
    // its nested libraries do not have to be unpacked into this build.
    compileOnly(variantOf(deps.create) { classifier("slim") }) {
        isTransitive = false
    }
    compileOnly(libs.ponder) {
        isTransitive = false
    }

    // Both optional at runtime: the ritual result type and the sequenced-assembly mixin that carry
    // a backpack's contents between tiers only load when their mods do.
    compileOnly(deps.forbiddenArcanus)
    compileOnly(deps.sophisticatedBackpacks)
    compileOnly(deps.sophisticatedCore)

    // The Triple Strip Cape hands out its extra slots from code, naming "back" directly; Curios'
    // own attribute event is the only way to move that to the cape slot without a mixin.
    compileOnly(deps.curios)
    testRuntimeOnly(deps.curios)

    // Hooked's item stat components and NeoForge item class - four new hook variants live in
    // this mod's namespace, and four built-in hooks get their default components rebalanced.
    compileOnly(deps.hooked)
    testRuntimeOnly(deps.hooked)
    testRuntimeOnly(deps.quark)
    testImplementation(deps.mekanism)
    testImplementation(deps.anvilcraft)
    testImplementation(deps.emi)

    compileOnly(deps.pocketDimension1201)
    gametestImplementation(deps.pocketDimension1201)
    gametestRuntimeOnly(deps.curios)
    gametestRuntimeOnly(deps.hooked)
    gametestRuntimeOnly(deps.apothicAttributes)
    gametestRuntimeOnly(deps.simplymore)
    gametestImplementation(deps.malum)
    gametestImplementation(deps.apotheosis)
    gametestImplementation(deps.enigmaticlegacy)
    gametestImplementation(deps.universalSawmill)
    gametestImplementation(deps.pastelMod)
    gametestRuntimeOnly(deps.l2Complements)
    gametestImplementation(deps.terraCurio)
    gametestImplementation(deps.ironsSpellsNSpellbooks)
    gametestRuntimeOnly(deps.acesSpellUtils)
    gametestRuntimeOnly(deps.discerningTheEldritch)
    gametestRuntimeOnly(deps.cataclysmSpellbooks)
    gametestRuntimeOnly(deps.lEndersCataclysm)
    gametestRuntimeOnly(deps.l2hostility)
    gametestRuntimeOnly(deps.malstone)
    gametestRuntimeOnly(deps.namelessTrinkets)
    gametestRuntimeOnly(deps.aquaculture)
    gametestRuntimeOnly(deps.armageddon)
    gametestRuntimeOnly(deps.antarchy)
    gametestRuntimeOnly(deps.mekanism)
    "integrationtestRuntimeOnly"(deps.anvilcraft)
    "integrationtestRuntimeOnly"(deps.curios)
    "integrationtestRuntimeOnly"(deps.hooked)
    "integrationtestRuntimeOnly"(deps.deeperdarker)
    "integrationtestImplementation"(deps.iceandfireCe)
    "integrationtestImplementation"(deps.forbiddenArcanus)
    "integrationtestImplementation"(deps.oritech)
    "integrationtestRuntimeOnly"(deps.pastelMod)
    "integrationtestRuntimeOnly"(deps.refinedStorage)
    "integrationtestRuntimeOnly"(deps.elementalMetalsMagicalMetallurgySeries)
    "integrationtestRuntimeOnly"(deps.magitech)
    "integrationtestRuntimeOnly"(deps.curseOfPandora)
    "integrationtestRuntimeOnly"(deps.l2Complements)
    "integrationtestImplementation"(deps.create)
    "integrationtestRuntimeOnly"(deps.cognition)
    "integrationtestRuntimeOnly"(deps.mekanism)
    "integrationtestRuntimeOnly"(deps.malum)
    "integrationtestRuntimeOnly"(deps.mythsAndLegends)
    "integrationtestRuntimeOnly"(deps.reAvaritia)
    "integrationtestRuntimeOnly"(project(":mods:hephaestus-architecture"))
    "integrationtestRuntimeOnly"(project(":mods:bertie-tiers"))
    gametestRuntimeOnly(deps.quark)
    gametestRuntimeOnly(deps.alexsMobs1211)
    gametestRuntimeOnly(deps.mythsAndLegends)
    gametestRuntimeOnly(deps.bossesrise)
    gametestRuntimeOnly(deps.piglinProliferation)
    gametestRuntimeOnly(deps.gensokyoDelightYoukaisFeasts)
    gametestRuntimeOnly(project(":mods:withering-waver"))
    gametestRuntimeOnly(project(":mods:berlords-better-horses"))
}
