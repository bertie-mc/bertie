plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.client-test")
}

dependencies {
    testImplementation(libs.mockito)
    compileOnly(deps.clothConfig)
    compileOnly(libs.jankson)
    compileOnly(deps.resourcefulConfig)
    compileOnly(deps.supermartijn642sConfigLib)
    compileOnly(libs.tomlkt)
    clienttestCompileOnly(deps.fzzyConfig)
    clienttestCompileOnly(deps.iceberg)
    clienttestCompileOnly(deps.owoLib)
    clienttestCompileOnly(deps.wunderlibNeoforge)

    clienttestRuntimeOnly(deps.artifacts)
    clienttestRuntimeOnly(deps.clothConfig)
    clienttestRuntimeOnly(deps.curios)
    clienttestRuntimeOnly(deps.fzzyConfig)
    clienttestRuntimeOnly(deps.iceberg)
    clienttestRuntimeOnly(deps.kotlinForForge)
    clienttestRuntimeOnly(deps.owoLib)
    clienttestRuntimeOnly(deps.resourcefulConfig)
    clienttestRuntimeOnly(deps.supermartijn642sConfigLib)
    clienttestRuntimeOnly(deps.wunderlibNeoforge)

    testImplementation(libs.jankson)
    testImplementation(deps.supermartijn642sConfigLib)
    testImplementation(libs.tomlkt)
}
