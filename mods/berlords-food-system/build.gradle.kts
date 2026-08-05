plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
}

dependencies {
    // Sophisticated Backpacks integration is optional at runtime.
    compileOnly(mods.sophisticatedCore)
}
