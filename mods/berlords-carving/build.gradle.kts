plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.gametest")
    id("bertie.client-test")
    id("bertie.dev-runs")
}

dependencies {
    compileOnly(deps.slagNEmbers)
    compileOnly(deps.emi)

    clienttestRuntimeOnly(deps.emi)
    clienttestRuntimeOnly(deps.slagNEmbers)
}
