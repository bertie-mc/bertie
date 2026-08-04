plugins {
    id("bertie.neoforge-mod")
    id("bertie.jvm-test")
}

dependencies {
    compileOnly(mods.forbiddenArcanus)
    compileOnly(mods.valhelsiaCore)
}
