plugins {
    id("bertie.neoforge-mod")
    id("bertie.neoforge-unit-test")
}

dependencies {
    // Sophisticated Backpacks integration is optional at runtime.
    compileOnly(mods.sophisticatedCore)
}
