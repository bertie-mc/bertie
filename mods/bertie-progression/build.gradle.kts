plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.dev-runs")
}

dependencies {
    compileOnly(deps.emi)
    runtimeOnly(deps.patchouli)

    // Ponder is published directly by Create's Maven; use the slim Create artifact so
    // its nested libraries do not have to be unpacked into this build.
    compileOnly(variantOf(deps.create) { classifier("slim") }) {
        isTransitive = false
    }
    compileOnly(libs.ponder) {
        isTransitive = false
    }

    // Both optional at runtime: the ritual result type and the sequenced-assembly mixin that carry
    // a backpack's contents between tiers only load when their mods do.
    compileOnly(deps.forbiddenArcanus)
    compileOnly(deps.sophisticatedBackpacks)
    compileOnly(deps.sophisticatedCore)

    // The Triple Strip Cape hands out its extra slots from code, naming "back" directly; Curios'
    // own attribute event is the only way to move that to the cape slot without a mixin.
    compileOnly(deps.curios)
    testRuntimeOnly(deps.curios)

    // Hooked's item stat components and NeoForge item class - four new hook variants live in
    // this mod's namespace, and four built-in hooks get their default components rebalanced.
    compileOnly(deps.hooked)
    testRuntimeOnly(deps.hooked)
}
