plugins {
    id("bertie.neoforge-mod")
    id("bertie.neoforge-unit-test")
    id("bertie.client-test")
    id("bertie.server-run")
}

dependencies {
    // Ponder is available directly from Create's Maven, so no nested-JAR extraction is
    // needed for its API or bundled Catnip classes.
    compileOnly(variantOf(libs.create.maven) { classifier("slim") }) {
        isTransitive = false
    }
    compileOnly(libs.ponder) {
        isTransitive = false
    }

    compileOnly(libs.forbidden.arcanus)
    compileOnly(libs.valhelsia.core)
    runtimeOnly(libs.forbidden.arcanus)
    runtimeOnly(libs.valhelsia.core)
}
