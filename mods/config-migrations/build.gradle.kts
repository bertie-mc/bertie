plugins {
    id("bertie.neoforge-unit-test")
    id("bertie.mockito-test")
    id("bertie.client-test")
}

dependencies {
    compileOnly(mods.clothConfig)
    compileOnly(libs.jankson)
    compileOnly(mods.resourcefulConfig)
    compileOnly(mods.supermartijn642sConfigLib)
    compileOnly(libs.tomlkt)
    clienttestCompileOnly(mods.fzzyConfig)
    clienttestCompileOnly(mods.iceberg)
    clienttestCompileOnly(mods.owoLib)
    clienttestCompileOnly(mods.wunderlibNeoforge)

    clienttestRuntimeOnly(mods.artifacts)
    clienttestRuntimeOnly(mods.clothConfig)
    clienttestRuntimeOnly(mods.curios)
    clienttestRuntimeOnly(mods.fzzyConfig)
    clienttestRuntimeOnly(mods.iceberg)
    clienttestRuntimeOnly(mods.kotlinForForge)
    clienttestRuntimeOnly(mods.owoLib)
    clienttestRuntimeOnly(mods.resourcefulConfig)
    clienttestRuntimeOnly(mods.supermartijn642sConfigLib)
    clienttestRuntimeOnly(mods.wunderlibNeoforge)

    testImplementation(libs.jankson)
    testImplementation(mods.supermartijn642sConfigLib)
    testImplementation(libs.tomlkt)
}
