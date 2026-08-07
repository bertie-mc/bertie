plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.gametest")
    id("bertie.dev-runs")
}

dependencies {
    // Ponder is available directly from Create's Maven, so no nested-JAR extraction is
    // needed for its API or bundled Catnip classes.
    compileOnly(variantOf(deps.create) { classifier("slim") }) {
        isTransitive = false
    }
    compileOnly(libs.ponder) {
        isTransitive = false
    }

    compileOnly(deps.forbiddenArcanus)
    runtimeOnly(deps.forbiddenArcanus)
}
