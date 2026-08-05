plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.dev-runs")
}

dependencies {
    compileOnly(mods.emi)

    // Ponder is published directly by Create's Maven; use the slim Create artifact so
    // its nested libraries do not have to be unpacked into this build.
    compileOnly(variantOf(mods.create) { classifier("slim") }) {
        isTransitive = false
    }
    compileOnly(libs.ponder) {
        isTransitive = false
    }
}
