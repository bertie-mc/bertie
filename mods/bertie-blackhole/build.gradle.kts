plugins {
    id("bertie.neoforge-mod")
    id("bertie.neoforge-unit-test")
    id("bertie.client-test")
    id("bertie.server-run")
}

dependencies {
    // The mixin plugin keeps the mod inert when this soft dependency is absent.
    compileOnly(libs.forbidden.arcanus)
}
