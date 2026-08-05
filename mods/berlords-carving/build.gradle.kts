plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.gametest")
    id("bertie.client-test")
    id("bertie.dev-runs")
}

dependencies {
    compileOnly(mods.slagNEmbers)
    compileOnly(mods.emi)

    clienttestRuntimeOnly(mods.emi)
    clienttestRuntimeOnly(mods.slagNEmbers)
}
