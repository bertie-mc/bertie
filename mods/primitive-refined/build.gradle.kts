plugins {
    id("bertie.neoforge-mod")
    id("bertie.neoforge-test-framework")
    id("bertie.mockito-test")
    id("bertie.server-run")
}

dependencies {
    // The pack supplies these hard dependencies. Compile against their precise API
    // artifacts without bundling or resolving unrelated transitives.
    compileOnly(variantOf(mods.create) { classifier("slim") }) {
        isTransitive = false
    }
    compileOnly(libs.ponder) {
        isTransitive = false
    }
    compileOnly(libs.flywheel) {
        isTransitive = false
    }
    compileOnly(mods.refinedStorage) {
        isTransitive = false
    }

    testImplementation(mods.refinedStorage) {
        isTransitive = false
    }
    testCompileOnly(variantOf(mods.create) { classifier("slim") }) {
        isTransitive = false
    }
    testCompileOnly(libs.ponder) {
        isTransitive = false
    }
    testCompileOnly(libs.flywheel) {
        isTransitive = false
    }
    testRuntimeOnly(mods.create) {
        isTransitive = false
    }
}
