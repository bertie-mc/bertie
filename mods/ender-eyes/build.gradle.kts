plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
}

dependencies {
    testImplementation(libs.neoforge.test.framework)
}
