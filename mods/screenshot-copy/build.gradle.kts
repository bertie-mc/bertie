plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.client-test")
    id("bertie.shaded-mod")
}

dependencies {
    shadedLibrary(libs.arboard)
    shadedLibrary(libs.native.utils)
}
