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
    add("clientTestCompileOnly", libs.fzzy.config)
    add("clientTestCompileOnly", "maven.modrinth:iceberg:IMssx9du")
    add("clientTestCompileOnly", "maven.modrinth:owo-lib:NMCHU6DZ")
    add("clientTestCompileOnly", "maven.modrinth:wunderlib-neoforge:5db3GZzg")
    testImplementation(libs.jankson)
    testImplementation(libs.supermartijn642.config.lib)
    testImplementation(libs.tomlkt)
}
