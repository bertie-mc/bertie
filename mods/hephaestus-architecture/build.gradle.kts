plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.gametest")
    id("bertie.dev-runs")
}

dependencies {
    // Ponder is available directly from Create's Maven, so no nested-JAR extraction is
    // needed for its API or bundled Catnip classes.
    compileOnly(variantOf(mods.create) { classifier("slim") }) {
        isTransitive = false
    }
    compileOnly(libs.ponder) {
        isTransitive = false
    }

    compileOnly(mods.forbiddenArcanus)
    compileOnly(mods.valhelsiaCore)
    runtimeOnly(mods.forbiddenArcanus)
    runtimeOnly(mods.valhelsiaCore)
}
