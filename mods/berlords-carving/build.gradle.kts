plugins {
    id("bertie.neoforge-mod")
    id("bertie.neoforge-unit-test")
    id("bertie.gametest")
    id("bertie.client-test")
    id("bertie.server-run")
}

dependencies {
    compileOnly(mods.slagNEmbers)
    compileOnly(mods.emi)

    clienttestRuntimeOnly(mods.emi)
    clienttestRuntimeOnly(mods.slagNEmbers)
}
