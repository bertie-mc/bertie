plugins {
    id("bertie.mod")
    id("bertie.gametest")
    id("bertie.dev-runs")
}

dependencies {
    compileOnly(deps.ironsSpellsNSpellbooks)
    runtimeOnly(deps.ironsSpellsNSpellbooks)

    gametestRuntimeOnly(deps.simplySwords)
}
