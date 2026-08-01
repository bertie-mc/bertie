plugins {
    id("bertie.neoforge-mod")
    id("bertie.neoforge-unit-test")
    id("bertie.client-test")
}

dependencies {
    // Sophisticated Backpacks integration is optional at runtime.
    compileOnly(libs.sophisticated.core)
}
