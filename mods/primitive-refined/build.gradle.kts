plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.dev-runs")
}

dependencies {
    testImplementation(libs.mockito)
    testImplementation("net.neoforged:testframework:${bertiePlatform.neoForge}")
    // The pack supplies these hard dependencies. Compile against their precise API
    // artifacts without bundling or resolving unrelated transitives.
    compileOnly(variantOf(deps.create) { classifier("slim") }) {
        isTransitive = false
    }
    compileOnly(libs.ponder) {
        isTransitive = false
    }
    compileOnly(libs.flywheel) {
        isTransitive = false
    }
    compileOnly(deps.refinedStorage) {
        isTransitive = false
    }
    runtimeOnly(deps.create)
    runtimeOnly(deps.refinedStorage)

    testImplementation(deps.refinedStorage) {
        isTransitive = false
    }
    testCompileOnly(variantOf(deps.create) { classifier("slim") }) {
        isTransitive = false
    }
    testCompileOnly(libs.ponder) {
        isTransitive = false
    }
    testCompileOnly(libs.flywheel) {
        isTransitive = false
    }
    testRuntimeOnly(deps.create) {
        isTransitive = false
    }
}
