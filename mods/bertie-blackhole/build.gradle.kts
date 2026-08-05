plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.dev-runs")
}

dependencies {
    // The mixin plugin keeps the mod inert when this soft dependency is absent.
    compileOnly(mods.forbiddenArcanus)
}
