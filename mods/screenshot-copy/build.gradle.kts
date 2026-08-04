plugins {
    id("bertie.neoforge-unit-test")
    id("bertie.client-test")
    id("bertie.embedded-library")
}

dependencies {
    embeddedLibrary(libs.arboard)
    embeddedLibrary(libs.native.utils)
}
