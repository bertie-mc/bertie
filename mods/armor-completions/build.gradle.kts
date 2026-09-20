plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.gametest")
}

dependencies {
    // Each piece extends its own set's armour item and renderer, so every source mod is on the
    // compile path and required at runtime: without one, that set stays unfinished.
    compileOnly(deps.borninchaos)
    compileOnly(deps.lEndersCataclysm)
    compileOnly(deps.hazenNStuff)
    compileOnly(deps.ironsSpellsNSpellbooks)
    compileOnly(deps.geckolib)

    runtimeOnly(deps.borninchaos)
    runtimeOnly(deps.lEndersCataclysm)
    runtimeOnly(deps.hazenNStuff)
    runtimeOnly(deps.ironsSpellsNSpellbooks)
    runtimeOnly(deps.geckolib)

    gametestImplementation(deps.borninchaos)
    gametestImplementation(deps.lEndersCataclysm)
    gametestImplementation(deps.hazenNStuff)
    gametestImplementation(deps.ironsSpellsNSpellbooks)
    gametestImplementation(deps.geckolib)

    // The model and motion tests bake the real geometry and drive the real renderer.
    testImplementation(deps.borninchaos)
    testImplementation(deps.lEndersCataclysm)
    testImplementation(deps.hazenNStuff)
    testImplementation(deps.ironsSpellsNSpellbooks)
    testImplementation(deps.geckolib)
    // Haze n Stuff's items reach into this one, so the tests that load them need it too.
    testRuntimeOnly(deps.acesSpellUtils)
    testRuntimeOnly(deps.apothicAttributes)
    testRuntimeOnly(deps.azurelib)
    testRuntimeOnly(deps.placebo)
    testImplementation("net.neoforged:testframework:${bertiePlatform.neoForge}")
}
