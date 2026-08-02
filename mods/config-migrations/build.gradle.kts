plugins {
    id("bertie.neoforge-unit-test")
    id("bertie.mockito-test")
    id("bertie.client-test")
}

dependencies {
    compileOnly(libs.cloth.config)
    compileOnly(libs.jankson)
    compileOnly(libs.resourceful.config)
    compileOnly(libs.supermartijn642.config.lib)
    compileOnly(libs.tomlkt)
    clientTestCompileOnly(libs.fzzy.config)
    clientTestCompileOnly(libs.iceberg)
    clientTestCompileOnly(libs.owo.lib)
    clientTestCompileOnly(libs.wunderlib.neoforge)
    testImplementation(libs.jankson)
    testImplementation(libs.supermartijn642.config.lib)
    testImplementation(libs.tomlkt)
}
