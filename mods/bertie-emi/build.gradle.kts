plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.jarjar")
    id("bertie.client-test")
}

dependencies {
    compileOnly(deps.emi)
    compileOnly(deps.slagNEmbers)
    compileOnly(variantOf(deps.create) { classifier("slim") }) {
        isTransitive = false
    }
    compileOnly(deps.lEndersCataclysm)
    compileOnly(deps.ironsSpellsNSpellbooks)
    compileOnly(deps.forbiddenArcanus)
    jarJarCompileOnly(deps.terraCurio)
    compileOnly(deps.enderio)
    compileOnly(deps.malum)
    compileOnly(deps.lodestonelib)
    compileOnly(deps.l2library)
    compileOnly(deps.extradelight)
    compileOnly(deps.dungeonsDelight)
    compileOnly(deps.expandedDelight)
    compileOnly(deps.avaritiasDelight)
    compileOnly(deps.farmersPizzeria)
    compileOnly(deps.farmersDelight)
    jarJarCompileOnly(deps.gensokyoDelightYoukaisFeasts)
    compileOnly(deps.cognition)
    compileOnly(deps.stellaris)
    compileOnly(deps.twilightDelight)
    compileOnly(deps.slavicDelight)
    compileOnly(deps.cuisineDelight)
    compileOnly(deps.berriesAndCherries)
    compileOnly(deps.betterArcheology)
    compileOnly(deps.l2Complements)
    jarJarCompileOnly(deps.anvilcraft)
    compileOnly(deps.magitech)
    compileOnly(deps.advancedLootInfo)

    testImplementation(deps.emi)

    clienttestRuntimeOnly(deps.emi)
    clienttestRuntimeOnly(deps.forbiddenArcanus)
}
