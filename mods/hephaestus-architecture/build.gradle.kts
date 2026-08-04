plugins {
    id("bertie.neoforge-mod")
    id("bertie.neoforge-unit-test")
    id("bertie.gametest")
    id("bertie.server-run")
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
