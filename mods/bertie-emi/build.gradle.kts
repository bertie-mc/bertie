plugins {
    id("bertie.neoforge-mod")
    id("bertie.neoforge-unit-test")
    id("bertie.jarjar-compile")
    id("bertie.client-test")
}

dependencies {
    compileOnly(mods.emi)
    compileOnly(mods.slagNEmbers)
    compileOnly(variantOf(mods.create) { classifier("slim") }) {
        isTransitive = false
    }
    compileOnly(mods.lEndersCataclysm)
    compileOnly(mods.ironsSpellsNSpellbooks)
    compileOnly(mods.forbiddenArcanus)
    jarJarCompileOnly(mods.terraCurio)
    compileOnly(mods.enderio)
    compileOnly(mods.malum)
    compileOnly(mods.lodestonelib)
    compileOnly(mods.l2library)
    compileOnly(mods.extradelight)
    compileOnly(mods.dungeonsDelight)
    compileOnly(mods.expandedDelight)
    compileOnly(mods.avaritiasDelight)
    compileOnly(mods.farmersPizzeria)
    compileOnly(mods.farmersDelight)
    jarJarCompileOnly(mods.gensokyoDelightYoukaisFeasts)
    compileOnly(mods.cognition)
    compileOnly(mods.stellaris)
    compileOnly(mods.twilightDelight)
    compileOnly(mods.slavicDelight)
    compileOnly(mods.cuisineDelight)
    compileOnly(mods.berriesAndCherries)
    compileOnly(mods.betterArcheology)
    compileOnly(mods.l2Complements)
    jarJarCompileOnly(mods.anvilcraft)
    compileOnly(mods.advancedLootInfo)

    testImplementation(mods.emi)

    clienttestRuntimeOnly(mods.emi)
    clienttestRuntimeOnly(mods.forbiddenArcanus)
    clienttestRuntimeOnly(mods.valhelsiaCore)
}
