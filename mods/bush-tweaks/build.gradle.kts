plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
}

dependencies {
    testImplementation(libs.mockito)
    testRuntimeOnly(deps.berriesAndCherries)
}
