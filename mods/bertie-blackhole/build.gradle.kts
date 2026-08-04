plugins {
    id("bertie.neoforge-mod")
    id("bertie.neoforge-unit-test")
    id("bertie.server-run")
}

dependencies {
    // The mixin plugin keeps the mod inert when this soft dependency is absent.
    compileOnly(mods.forbiddenArcanus)
}
