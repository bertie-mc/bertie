plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.gametest")
    id("bertie.dev-runs")
}

dependencies {
    compileOnly(deps.ironsSpellsNSpellbooks)
    runtimeOnly(deps.ironsSpellsNSpellbooks)

    // The Occult and Abyssal manuscripts research schools these addons register. Cataclysm:
    // Spellbooks loads L_Ender's Cataclysm classes although its metadata marks the mod optional.
    gametestRuntimeOnly(deps.cataclysmSpellbooks)
    gametestRuntimeOnly(deps.discerningTheEldritch)
    gametestRuntimeOnly(deps.lEndersCataclysm)
}

tasks.named<Jar>("jar") {
    from(layout.projectDirectory.file("LICENSE")) { into("META-INF") }
}
