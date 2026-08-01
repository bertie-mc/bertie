plugins {
    id("bertie.neoforge-mod")
    id("bertie.neoforge-test-framework")
    id("bertie.mockito-test")
    id("bertie.server-run")
}

dependencies {
    // The pack supplies these hard dependencies. Compile against their precise API
    // artifacts without bundling or resolving unrelated transitives.
    compileOnly("com.simibubi.create:create-1.21.1:${libs.versions.create.maven.get()}:slim") {
        isTransitive = false
    }
    compileOnly(libs.ponder) {
        isTransitive = false
    }
    compileOnly(libs.flywheel) {
        isTransitive = false
    }
    compileOnly(libs.refined.storage) {
        isTransitive = false
    }

    testImplementation(libs.refined.storage) {
        isTransitive = false
    }
    testCompileOnly("com.simibubi.create:create-1.21.1:${libs.versions.create.maven.get()}:slim") {
        isTransitive = false
    }
    testCompileOnly(libs.ponder) {
        isTransitive = false
    }
    testCompileOnly(libs.flywheel) {
        isTransitive = false
    }
    testRuntimeOnly(libs.create.maven) {
        isTransitive = false
    }
}
