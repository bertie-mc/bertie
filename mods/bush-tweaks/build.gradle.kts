plugins {
    id("bertie.neoforge-mod")
    id("bertie.neoforge-unit-test")
    id("bertie.mockito-test")
}

dependencies {
    runtimeOnly(libs.berries.and.cherries)
}
