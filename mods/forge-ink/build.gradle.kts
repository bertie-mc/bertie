plugins {
    id("bertie.neoforge-mod")
    id("bertie.jvm-test")
}

dependencies {
    compileOnly(libs.forbidden.arcanus)
    compileOnly(libs.valhelsia.core)
}
