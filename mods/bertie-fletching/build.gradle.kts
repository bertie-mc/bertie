plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.gametest")
}

dependencies {
    compileOnly(deps.fletcheryExpanded)
    runtimeOnly(deps.fletcheryExpanded)
    testImplementation(deps.fletcheryExpanded)
    gametestImplementation(deps.fletcheryExpanded)
}
