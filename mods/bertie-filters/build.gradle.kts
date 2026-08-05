plugins {
    id("bertie.mod")
    id("bertie.dev-runs")
}

dependencies {
    compileOnly(mods.ftbFilterSystem)
    compileOnly(mods.architecturyApi)
    compileOnly(mods.slagNEmbers)
}
