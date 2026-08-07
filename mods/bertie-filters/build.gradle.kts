plugins {
    id("bertie.mod")
    id("bertie.dev-runs")
}

dependencies {
    compileOnly(deps.ftbFilterSystem)
    compileOnly(deps.architecturyApi)
    compileOnly(deps.slagNEmbers)
    runtimeOnly(deps.ftbFilterSystem)
    runtimeOnly(deps.architecturyApi)
}
