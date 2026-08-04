plugins {
    id("bertie.neoforge-mod")
    id("bertie.jvm-test")
    id("bertie.server-run")
}

dependencies {
    compileOnly(mods.ftbFilterSystem)
    compileOnly(mods.architecturyApi)
    compileOnly(mods.slagNEmbers)
}
