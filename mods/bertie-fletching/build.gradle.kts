plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.gametest")
    id("bertie.client-test")
}

dependencies {
    compileOnly(deps.fletcheryExpanded)
    runtimeOnly(deps.fletcheryExpanded)
    testImplementation(deps.fletcheryExpanded)
    gametestImplementation(deps.fletcheryExpanded)
    clienttestImplementation(deps.fletcheryExpanded)
}
